package nl.mnsw.formality.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import nl.mnsw.config.PulsarConfig
import nl.mnsw.formality.application.SubmitFormalityCommand
import nl.mnsw.formality.application.SubmitFormalityUseCase
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.shared.exception.ValidationException
import nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.pulsar.annotation.PulsarListener
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Pulsar consumer voor inkomende RIM-berichten.
 * Luistert op alle vijf RIM inbound topics (NOA, NOS, NOD, VID, SID).
 *
 * Bij ValidationException: het bericht wordt geslikt (niet opnieuw verwerkt).
 * De formality wordt als REJECTED opgeslagen via SubmitFormalityUseCase — toekomstige fase.
 * Voor nu: gelogd en acknowledged (Spring Pulsar ack bij geen exception).
 *
 * Bij overige exceptions: wordt opnieuw gegooid zodat Spring Pulsar negative-acknowledges
 * en het bericht na backoff opnieuw probeert.
 *
 * De submitterId wordt bepaald via het senderEori-veld in het bericht: in fase 1 wordt
 * een configureerbare RIM system-user UUID gebruikt als fallback.
 * Toekomstige fase: opzoeken van gebruiker op EORI via UserRepository.
 */
@Service
class RimPulsarConsumer(
    private val submitFormalityUseCase: SubmitFormalityUseCase,
    private val visitRepository: VisitRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${mnsw.rim.system-user-id:00000000-0000-0000-0000-000000000001}")
    private val rimSystemUserId: UUID
) {

    private val logger = LoggerFactory.getLogger(RimPulsarConsumer::class.java)

    @PulsarListener(
        topics = [PulsarConfig.TOPIC_RIM_INBOUND_NOA],
        subscriptionName = "mnsw-rim-noa-subscription"
    )
    fun consumeNoa(message: String) = processRimMessage(message, FormalityType.NOA)

    @PulsarListener(
        topics = [PulsarConfig.TOPIC_RIM_INBOUND_NOS],
        subscriptionName = "mnsw-rim-nos-subscription"
    )
    fun consumeNos(message: String) = processRimMessage(message, FormalityType.NOS)

    @PulsarListener(
        topics = [PulsarConfig.TOPIC_RIM_INBOUND_NOD],
        subscriptionName = "mnsw-rim-nod-subscription"
    )
    fun consumeNod(message: String) = processRimMessage(message, FormalityType.NOD)

    @PulsarListener(
        topics = [PulsarConfig.TOPIC_RIM_INBOUND_VID],
        subscriptionName = "mnsw-rim-vid-subscription"
    )
    fun consumeVid(message: String) = processRimMessage(message, FormalityType.VID)

    @PulsarListener(
        topics = [PulsarConfig.TOPIC_RIM_INBOUND_SID],
        subscriptionName = "mnsw-rim-sid-subscription"
    )
    fun consumeSid(message: String) = processRimMessage(message, FormalityType.SID)

    private fun processRimMessage(rawMessage: String, type: FormalityType) {
        try {
            val rimMessage = objectMapper.readValue(rawMessage, RimInboundMessage::class.java)
            logger.info(
                "RIM bericht ontvangen: messageIdentifier={}, type={}, senderEori={}",
                rimMessage.messageIdentifier, type, rimMessage.senderEori
            )

            val visitEntity = findOrCreateVisit(rimMessage.visitInfo)
            val payload = mapToFormalityPayload(type, rimMessage)

            val command = SubmitFormalityCommand(
                visitId = visitEntity.id,
                type = type,
                lrn = rimMessage.lrn,
                messageIdentifier = rimMessage.messageIdentifier,
                payload = payload,
                submitterId = rimSystemUserId,
                channel = SubmissionChannel.RIM
            )

            val result = submitFormalityUseCase.execute(command)
            logger.info(
                "RIM bericht succesvol verwerkt: messageIdentifier={}, formalityId={}",
                rimMessage.messageIdentifier, result.formalityId
            )
            // Bij succes: Spring Pulsar acknowledges het bericht automatisch (geen exception)

        } catch (e: ValidationException) {
            // Validatiefout: het bericht is inhoudelijk ongeldig.
            // Acknowledge zodat het bericht NIET opnieuw wordt verwerkt (het zal altijd falen).
            // Toekomstige fase: stuur FRM-response met status REJECTED terug naar indiener.
            logger.warn(
                "RIM bericht afgewezen wegens validatiefout (type={}): {} fout(en): {}",
                type, e.errors.size, e.errors.joinToString { "${it.field}: ${it.code}" }
            )
            // Slik de exception — Spring Pulsar acknowledges het bericht
        } catch (e: Exception) {
            // Technische fout: negative-acknowledge, Spring Pulsar herprobeert na backoff.
            logger.error(
                "Technische fout bij verwerken RIM bericht (type={}): {}",
                type, e.message, e
            )
            throw e
        }
    }

    /**
     * Zoek een bestaande visit op basis van IMO-nummer en haven, of maak een nieuwe aan.
     * In fase 1: eerste match op imoNumber + portLocode wordt gebruikt.
     * Toekomstige fase: deduplicatie op basis van IMO + ETA/ETD-venster.
     */
    private fun findOrCreateVisit(visitInfo: RimVisitInfo): VisitJpaEntity {
        val existing = visitRepository.findByImoNumber(visitInfo.imoNumber)
            .firstOrNull { it.portLocode == visitInfo.portLocode }

        if (existing != null) {
            logger.debug(
                "Bestaande visit gevonden: id={}, imoNumber={}, portLocode={}",
                existing.id, visitInfo.imoNumber, visitInfo.portLocode
            )
            return existing
        }

        val newVisit = VisitJpaEntity(
            id = UUID.randomUUID(),
            imoNumber = visitInfo.imoNumber,
            vesselName = visitInfo.vesselName,
            portLocode = visitInfo.portLocode,
            eta = visitInfo.eta?.let { OffsetDateTime.parse(it) },
            etd = visitInfo.etd?.let { OffsetDateTime.parse(it) },
            createdAt = OffsetDateTime.now()
        )
        val saved = visitRepository.save(newVisit)
        logger.info(
            "Nieuwe visit aangemaakt voor RIM bericht: id={}, imoNumber={}, portLocode={}",
            saved.id, visitInfo.imoNumber, visitInfo.portLocode
        )
        return saved
    }

    /**
     * Mapt de generieke payload-map van een RimInboundMessage naar een type-specifieke FormalityPayload.
     * De payload-velden worden gelezen uit de Map<String, Any> in het RIM-bericht.
     */
    private fun mapToFormalityPayload(type: FormalityType, rimMessage: RimInboundMessage): FormalityPayload {
        val payload = rimMessage.payload
        return when (type) {
            FormalityType.NOA -> FormalityPayload.NoaPayload(
                expectedArrival = parseOffsetDateTime(payload["expectedArrival"]),
                lastPortLocode = payload["lastPortLocode"] as? String,
                nextPortLocode = payload["nextPortLocode"] as? String,
                purposeOfCall = payload["purposeOfCall"] as? String,
                personsOnBoard = (payload["personsOnBoard"] as? Number)?.toInt(),
                dangerousGoods = (payload["dangerousGoods"] as? Boolean) ?: false,
                wasteDelivery = (payload["wasteDelivery"] as? Boolean) ?: false,
                maxStaticDraught = (payload["maxStaticDraught"] as? Number)?.let {
                    java.math.BigDecimal(it.toString())
                }
            )
            FormalityType.NOS -> FormalityPayload.NosPayload(
                actualSailing = parseOffsetDateTime(payload["actualSailing"]),
                nextPortLocode = payload["nextPortLocode"] as? String,
                destinationCountry = payload["destinationCountry"] as? String
            )
            FormalityType.NOD -> FormalityPayload.NodPayload(
                expectedDeparture = parseOffsetDateTime(payload["expectedDeparture"]),
                nextPortLocode = payload["nextPortLocode"] as? String,
                destinationCountry = payload["destinationCountry"] as? String,
                lastCargoOperations = (payload["lastCargoOperations"] as? String)?.let {
                    OffsetDateTime.parse(it)
                }
            )
            FormalityType.VID -> FormalityPayload.VidPayload(
                certificateNationality = payload["certificateNationality"] as? String,
                grossTonnage = (payload["grossTonnage"] as? Number)?.let {
                    java.math.BigDecimal(it.toString())
                },
                netTonnage = (payload["netTonnage"] as? Number)?.let {
                    java.math.BigDecimal(it.toString())
                },
                deadweight = (payload["deadweight"] as? Number)?.let {
                    java.math.BigDecimal(it.toString())
                },
                lengthOverall = (payload["lengthOverall"] as? Number)?.let {
                    java.math.BigDecimal(it.toString())
                },
                shipType = payload["shipType"] as? String,
                callSign = payload["callSign"] as? String,
                mmsi = payload["mmsi"] as? String
            )
            FormalityType.SID -> FormalityPayload.SidPayload(
                ispsLevel = (payload["ispsLevel"] as? Number)?.toInt()
                    ?: throw ValidationException(
                        listOf(nl.mnsw.shared.exception.FieldError("ispsLevel", "REQUIRED", "ispsLevel is verplicht"))
                    ),
                last10Ports = emptyList(), // Toekomstige fase: deserialiseer PortEntry lijst
                securityDeclaration = payload["securityDeclaration"] as? String,
                shipToShipActivities = (payload["shipToShipActivities"] as? Boolean) ?: false,
                designatedAuthority = payload["designatedAuthority"] as? String,
                ssasActivated = (payload["ssasActivated"] as? Boolean) ?: false
            )
        }
    }

    private fun parseOffsetDateTime(value: Any?): OffsetDateTime {
        return when (value) {
            is String -> OffsetDateTime.parse(value)
            null -> throw ValidationException(
                listOf(nl.mnsw.shared.exception.FieldError("datetime", "REQUIRED", "Verplicht datumveld ontbreekt"))
            )
            else -> throw ValidationException(
                listOf(nl.mnsw.shared.exception.FieldError("datetime", "INVALID_FORMAT", "Ongeldig datumformaat: $value"))
            )
        }
    }
}

/**
 * Inkomend RIM-bericht van Apache Pulsar.
 * Conform het Pulsar Message Schema voor RIM inbound berichten (design.md).
 */
data class RimInboundMessage(
    val messageIdentifier: String,
    val lrn: String?,
    val senderEori: String,
    val visitInfo: RimVisitInfo,
    val payload: Map<String, Any>,
    val receivedAt: String
)

/**
 * Scheepsgegevens in een RIM-bericht — gebruikt voor visit-correlatie of aanmaak.
 */
data class RimVisitInfo(
    val imoNumber: String,
    val vesselName: String,
    val portLocode: String,
    val eta: String?,
    val etd: String?
)
