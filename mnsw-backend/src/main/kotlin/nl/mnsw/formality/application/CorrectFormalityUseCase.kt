package nl.mnsw.formality.application

import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityValidator
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.FormalityRepository
import nl.mnsw.formality.infrastructure.persistence.NoaPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NoaPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.NodPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NodPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.NosPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NosPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.SidPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.SidPayloadRepository
import nl.mnsw.formality.infrastructure.persistence.VidPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.VidPayloadRepository
import nl.mnsw.shared.exception.ConcurrentCorrectionException
import nl.mnsw.shared.exception.FormalityNotFoundException
import nl.mnsw.shared.exception.UnauthorizedAccessException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Use case voor het indienen van een correctie op een bestaande formality.
 * Conform de correctieworkflow: maakt een nieuw versie-record aan, markeert het origineel als SUPERSEDED.
 *
 * Autorisatieregels:
 *  - Alleen de originele indiener mag corrigeren (submitterId == origineel.submitterId)
 *  - Er mag geen actievere versie bestaan (concurrent correction check)
 *
 * Transactiegrens: volledige use case in één transactie.
 * Pulsar-publicatie buiten transactie via @TransactionalEventListener AFTER_COMMIT.
 */
@Service
@Transactional
class CorrectFormalityUseCase(
    private val formalityRepository: FormalityRepository,
    private val formalityValidator: FormalityValidator,
    private val noaPayloadRepository: NoaPayloadRepository,
    private val nosPayloadRepository: NosPayloadRepository,
    private val nodPayloadRepository: NodPayloadRepository,
    private val vidPayloadRepository: VidPayloadRepository,
    private val sidPayloadRepository: SidPayloadRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun execute(command: CorrectFormalityCommand): CorrectFormalityResult {
        // 1. Haal originele formality op
        val original = formalityRepository.findById(command.originalFormalityId).orElseThrow {
            FormalityNotFoundException(command.originalFormalityId)
        }

        // 2. Controleer dat indiener overeenkomt met originele indiener
        if (original.submitter.id != command.submitterId) {
            throw UnauthorizedAccessException(
                "Alleen de originele indiener mag een correctie indienen. " +
                    "Indiener: ${command.submitterId}, origineel: ${original.submitter.id}"
            )
        }

        // 3. Controleer dat er geen actievere versie bestaat (concurrent correction guard)
        val activeVersion = formalityRepository.findCurrentVersionByVisitIdAndType(
            original.visit.id,
            original.type
        )
        if (activeVersion != null && activeVersion.id != original.id) {
            throw ConcurrentCorrectionException(command.originalFormalityId)
        }

        // 4. Valideer nieuwe payload
        formalityValidator.validate(original.type, command.payload, original.visit.imoNumber, original.visit.portLocode)

        // 5. Maak nieuwe formality aan (version = origineel.version + 1)
        val newFormalityId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val newFormality = FormalityJpaEntity(
            id = newFormalityId,
            visit = original.visit,
            type = original.type,
            version = original.version + 1,
            status = FormalityStatus.SUBMITTED,
            submitter = original.submitter,
            lrn = command.lrn,
            messageIdentifier = command.messageIdentifier,
            submittedAt = now,
            supersededBy = null,
            channel = original.channel
        )

        formalityRepository.save(newFormality)

        // 6. Sla nieuwe payload op
        savePayload(newFormalityId, command.payload, newFormality)

        // 7. Update origineel: supersededBy = nieuw id, status = SUPERSEDED
        original.supersededBy = newFormalityId
        original.status = FormalityStatus.SUPERSEDED
        formalityRepository.save(original)

        // 8. Publiceer event (after-commit)
        applicationEventPublisher.publishEvent(
            FormalitySubmittedEvent(
                formalityId = newFormalityId,
                visitId = original.visit.id,
                type = original.type,
                portLocode = original.visit.portLocode,
                submittedAt = now,
                channel = original.channel,
                submitterId = command.submitterId,
                messageIdentifier = command.messageIdentifier
            )
        )

        return CorrectFormalityResult(
            newFormalityId = newFormalityId,
            messageIdentifier = command.messageIdentifier,
            version = original.version + 1,
            status = FormalityStatus.SUBMITTED,
            previousVersionId = command.originalFormalityId
        )
    }

    private fun savePayload(formalityId: UUID, payload: FormalityPayload, formalityEntity: FormalityJpaEntity) {
        when (payload) {
            is FormalityPayload.NoaPayload -> noaPayloadRepository.save(
                NoaPayloadJpaEntity(
                    id = formalityId,
                    formality = formalityEntity,
                    expectedArrival = payload.expectedArrival,
                    lastPortLocode = payload.lastPortLocode,
                    nextPortLocode = payload.nextPortLocode,
                    purposeOfCall = payload.purposeOfCall,
                    personsOnBoard = payload.personsOnBoard,
                    dangerousGoods = payload.dangerousGoods,
                    wasteDelivery = payload.wasteDelivery,
                    maxStaticDraught = payload.maxStaticDraught
                )
            )
            is FormalityPayload.NosPayload -> nosPayloadRepository.save(
                NosPayloadJpaEntity(
                    id = formalityId,
                    formality = formalityEntity,
                    actualSailing = payload.actualSailing,
                    nextPortLocode = payload.nextPortLocode,
                    destinationCountry = payload.destinationCountry
                )
            )
            is FormalityPayload.NodPayload -> nodPayloadRepository.save(
                NodPayloadJpaEntity(
                    id = formalityId,
                    formality = formalityEntity,
                    expectedDeparture = payload.expectedDeparture,
                    nextPortLocode = payload.nextPortLocode,
                    destinationCountry = payload.destinationCountry,
                    lastCargoOperations = payload.lastCargoOperations
                )
            )
            is FormalityPayload.VidPayload -> vidPayloadRepository.save(
                VidPayloadJpaEntity(
                    id = formalityId,
                    formality = formalityEntity,
                    certificateNationality = payload.certificateNationality,
                    grossTonnage = payload.grossTonnage,
                    netTonnage = payload.netTonnage,
                    deadweight = payload.deadweight,
                    lengthOverall = payload.lengthOverall,
                    shipType = payload.shipType,
                    callSign = payload.callSign,
                    mmsi = payload.mmsi
                )
            )
            is FormalityPayload.SidPayload -> sidPayloadRepository.save(
                SidPayloadJpaEntity(
                    id = formalityId,
                    formality = formalityEntity,
                    ispsLevel = payload.ispsLevel,
                    last10Ports = payload.last10Ports,
                    securityDeclaration = payload.securityDeclaration,
                    shipToShipActivities = payload.shipToShipActivities,
                    designatedAuthority = payload.designatedAuthority,
                    ssasActivated = payload.ssasActivated
                )
            )
        }
    }
}

data class CorrectFormalityCommand(
    val originalFormalityId: UUID,
    val lrn: String?,
    val messageIdentifier: String,
    val payload: FormalityPayload,
    val submitterId: UUID
)

data class CorrectFormalityResult(
    val newFormalityId: UUID,
    val messageIdentifier: String,
    val version: Int,
    val status: FormalityStatus,
    val previousVersionId: UUID
)
