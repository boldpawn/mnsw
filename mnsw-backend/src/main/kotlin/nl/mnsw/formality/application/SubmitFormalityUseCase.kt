package nl.mnsw.formality.application

import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
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
import nl.mnsw.shared.exception.VisitNotFoundException
import nl.mnsw.visit.infrastructure.persistence.VisitRepository
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Use case voor het indienen van een nieuwe formality.
 * Toegestaan voor SCHEEPSAGENT, LADINGAGENT, ADMIN.
 *
 * Transactiegrens: stap 1-3 in één transactie.
 * Pulsar-publicatie (stap 4) buiten transactie via @TransactionalEventListener AFTER_COMMIT.
 */
@Service
@Transactional
class SubmitFormalityUseCase(
    private val visitRepository: VisitRepository,
    private val formalityRepository: FormalityRepository,
    private val formalityValidator: FormalityValidator,
    private val noaPayloadRepository: NoaPayloadRepository,
    private val nosPayloadRepository: NosPayloadRepository,
    private val nodPayloadRepository: NodPayloadRepository,
    private val vidPayloadRepository: VidPayloadRepository,
    private val sidPayloadRepository: SidPayloadRepository,
    private val userRepository: UserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun execute(command: SubmitFormalityCommand): SubmitFormalityResult {
        // 1. Haal Visit op
        val visitEntity = visitRepository.findById(command.visitId).orElseThrow {
            VisitNotFoundException(command.visitId)
        }

        // 2. Valideer payload conform MIG-regels
        formalityValidator.validate(command.type, command.payload, visitEntity.imoNumber, visitEntity.portLocode)

        // 3. Haal submitter op
        val submitterEntity = userRepository.findById(command.submitterId).orElseThrow {
            IllegalStateException("Submitter not found: ${command.submitterId}")
        }

        // 4. Maak formality JPA entity aan
        val formalityId = UUID.randomUUID()
        val now = OffsetDateTime.now()

        val formalityEntity = FormalityJpaEntity(
            id = formalityId,
            visit = visitEntity,
            type = command.type,
            version = 1,
            status = FormalityStatus.SUBMITTED,
            submitter = submitterEntity,
            lrn = command.lrn,
            messageIdentifier = command.messageIdentifier,
            submittedAt = now,
            supersededBy = null,
            channel = command.channel
        )

        val savedFormality = formalityRepository.save(formalityEntity)

        // 5. Sla type-specifieke payload op
        savePayload(formalityId, command.payload, savedFormality)

        // 6. Publiceer event (after-commit — Pulsar producer luistert via @TransactionalEventListener)
        applicationEventPublisher.publishEvent(
            FormalitySubmittedEvent(
                formalityId = formalityId,
                visitId = command.visitId,
                type = command.type,
                portLocode = visitEntity.portLocode,
                submittedAt = now,
                channel = command.channel,
                submitterId = command.submitterId,
                messageIdentifier = command.messageIdentifier
            )
        )

        return SubmitFormalityResult(
            formalityId = formalityId,
            messageIdentifier = command.messageIdentifier,
            status = FormalityStatus.SUBMITTED
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

data class SubmitFormalityCommand(
    val visitId: UUID,
    val type: FormalityType,
    val lrn: String?,
    val messageIdentifier: String,
    val payload: FormalityPayload,
    val submitterId: UUID,
    val channel: SubmissionChannel
)

data class SubmitFormalityResult(
    val formalityId: UUID,
    val messageIdentifier: String,
    val status: FormalityStatus
)
