package nl.mnsw.formality.infrastructure.web

import nl.mnsw.formality.domain.Formality
import nl.mnsw.formality.domain.FormalityStatus
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.domain.payload.FormalityPayload
import nl.mnsw.formality.domain.payload.PortEntry
import nl.mnsw.formality.infrastructure.persistence.FormalityJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NodPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NoaPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.NosPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.SidPayloadJpaEntity
import nl.mnsw.formality.infrastructure.persistence.VidPayloadJpaEntity
import nl.mnsw.shared.exception.ValidationException
import nl.mnsw.shared.exception.FieldError
import org.springframework.stereotype.Component

/**
 * Mapper tussen FormalityJpaEntity (infrastructuurlaag) en Formality (domeinlaag).
 * Gebruikt sealed class dispatch (when) voor type-specifieke payload mapping.
 * Geen business logica in de mapper — uitsluitend structurele transformatie.
 */
@Component
class FormalityMapper {

    /**
     * Mapt een FormalityJpaEntity naar een Formality domain object.
     * De correcte payload-entiteit wordt bepaald op basis van entity.type.
     *
     * @throws ValidationException wanneer de verwachte payload ontbreekt in de entiteit
     */
    fun toDomain(entity: FormalityJpaEntity): Formality {
        val payload = when (entity.type) {
            FormalityType.NOA -> {
                val noa = entity.noaPayload
                    ?: throw ValidationException(listOf(FieldError("payload", "MISSING_NOA_PAYLOAD", "NOA payload ontbreekt voor formality ${entity.id}")))
                FormalityPayload.NoaPayload(
                    expectedArrival = noa.expectedArrival,
                    lastPortLocode = noa.lastPortLocode,
                    nextPortLocode = noa.nextPortLocode,
                    purposeOfCall = noa.purposeOfCall,
                    personsOnBoard = noa.personsOnBoard,
                    dangerousGoods = noa.dangerousGoods,
                    wasteDelivery = noa.wasteDelivery,
                    maxStaticDraught = noa.maxStaticDraught
                )
            }
            FormalityType.NOS -> {
                val nos = entity.nosPayload
                    ?: throw ValidationException(listOf(FieldError("payload", "MISSING_NOS_PAYLOAD", "NOS payload ontbreekt voor formality ${entity.id}")))
                FormalityPayload.NosPayload(
                    actualSailing = nos.actualSailing,
                    nextPortLocode = nos.nextPortLocode,
                    destinationCountry = nos.destinationCountry
                )
            }
            FormalityType.NOD -> {
                val nod = entity.nodPayload
                    ?: throw ValidationException(listOf(FieldError("payload", "MISSING_NOD_PAYLOAD", "NOD payload ontbreekt voor formality ${entity.id}")))
                FormalityPayload.NodPayload(
                    expectedDeparture = nod.expectedDeparture,
                    nextPortLocode = nod.nextPortLocode,
                    destinationCountry = nod.destinationCountry,
                    lastCargoOperations = nod.lastCargoOperations
                )
            }
            FormalityType.VID -> {
                val vid = entity.vidPayload
                    ?: throw ValidationException(listOf(FieldError("payload", "MISSING_VID_PAYLOAD", "VID payload ontbreekt voor formality ${entity.id}")))
                FormalityPayload.VidPayload(
                    certificateNationality = vid.certificateNationality,
                    grossTonnage = vid.grossTonnage,
                    netTonnage = vid.netTonnage,
                    deadweight = vid.deadweight,
                    lengthOverall = vid.lengthOverall,
                    shipType = vid.shipType,
                    callSign = vid.callSign,
                    mmsi = vid.mmsi
                )
            }
            FormalityType.SID -> {
                val sid = entity.sidPayload
                    ?: throw ValidationException(listOf(FieldError("payload", "MISSING_SID_PAYLOAD", "SID payload ontbreekt voor formality ${entity.id}")))
                FormalityPayload.SidPayload(
                    ispsLevel = sid.ispsLevel,
                    last10Ports = sid.last10Ports,
                    securityDeclaration = sid.securityDeclaration,
                    shipToShipActivities = sid.shipToShipActivities,
                    designatedAuthority = sid.designatedAuthority,
                    ssasActivated = sid.ssasActivated
                )
            }
        }

        return Formality(
            id = entity.id,
            visitId = entity.visit.id,
            type = entity.type,
            version = entity.version,
            status = entity.status,
            submitterId = entity.submitter.id,
            lrn = entity.lrn,
            messageIdentifier = entity.messageIdentifier,
            submittedAt = entity.submittedAt,
            supersededBy = entity.supersededBy,
            channel = entity.channel,
            payload = payload
        )
    }

    /**
     * Mapt een Formality domain object naar een FormalityJpaEntity.
     * De visit en submitter entiteiten moeten separaat worden opgezocht en worden doorgegeven.
     * Payload-entiteiten worden aangemaakt en teruggegeven als onderdeel van het resultaat.
     *
     * @return Pair van (FormalityJpaEntity, payload-entiteit)
     */
    fun toEntity(
        domain: Formality,
        visitEntity: nl.mnsw.visit.infrastructure.persistence.VisitJpaEntity,
        submitterEntity: nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
    ): FormalityJpaEntity {
        val entity = FormalityJpaEntity(
            id = domain.id,
            visit = visitEntity,
            type = domain.type,
            version = domain.version,
            status = domain.status,
            submitter = submitterEntity,
            lrn = domain.lrn,
            messageIdentifier = domain.messageIdentifier,
            submittedAt = domain.submittedAt,
            supersededBy = domain.supersededBy,
            channel = domain.channel
        )

        // Koppel de juiste payload-entiteit
        when (val payload = domain.payload) {
            is FormalityPayload.NoaPayload -> {
                entity.noaPayload = NoaPayloadJpaEntity(
                    id = domain.id,
                    formality = entity,
                    expectedArrival = payload.expectedArrival,
                    lastPortLocode = payload.lastPortLocode,
                    nextPortLocode = payload.nextPortLocode,
                    purposeOfCall = payload.purposeOfCall,
                    personsOnBoard = payload.personsOnBoard,
                    dangerousGoods = payload.dangerousGoods,
                    wasteDelivery = payload.wasteDelivery,
                    maxStaticDraught = payload.maxStaticDraught
                )
            }
            is FormalityPayload.NosPayload -> {
                entity.nosPayload = NosPayloadJpaEntity(
                    id = domain.id,
                    formality = entity,
                    actualSailing = payload.actualSailing,
                    nextPortLocode = payload.nextPortLocode,
                    destinationCountry = payload.destinationCountry
                )
            }
            is FormalityPayload.NodPayload -> {
                entity.nodPayload = NodPayloadJpaEntity(
                    id = domain.id,
                    formality = entity,
                    expectedDeparture = payload.expectedDeparture,
                    nextPortLocode = payload.nextPortLocode,
                    destinationCountry = payload.destinationCountry,
                    lastCargoOperations = payload.lastCargoOperations
                )
            }
            is FormalityPayload.VidPayload -> {
                entity.vidPayload = VidPayloadJpaEntity(
                    id = domain.id,
                    formality = entity,
                    certificateNationality = payload.certificateNationality,
                    grossTonnage = payload.grossTonnage,
                    netTonnage = payload.netTonnage,
                    deadweight = payload.deadweight,
                    lengthOverall = payload.lengthOverall,
                    shipType = payload.shipType,
                    callSign = payload.callSign,
                    mmsi = payload.mmsi
                )
            }
            is FormalityPayload.SidPayload -> {
                entity.sidPayload = SidPayloadJpaEntity(
                    id = domain.id,
                    formality = entity,
                    ispsLevel = payload.ispsLevel,
                    last10Ports = payload.last10Ports,
                    securityDeclaration = payload.securityDeclaration,
                    shipToShipActivities = payload.shipToShipActivities,
                    designatedAuthority = payload.designatedAuthority,
                    ssasActivated = payload.ssasActivated
                )
            }
        }

        return entity
    }
}
