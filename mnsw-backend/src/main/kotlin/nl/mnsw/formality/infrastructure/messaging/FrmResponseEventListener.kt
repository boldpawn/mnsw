package nl.mnsw.formality.infrastructure.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.mnsw.config.PulsarConfig
import nl.mnsw.formality.application.FrmResponseCreatedEvent
import nl.mnsw.formality.domain.SubmissionChannel
import org.slf4j.LoggerFactory
import org.springframework.pulsar.core.PulsarTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

/**
 * Luistert op FrmResponseCreatedEvent en publiceert de FRM naar het Pulsar outbound-topic,
 * maar alleen voor RIM-indieners.
 *
 * Webindieners (channel=WEB) ontvangen de FRM-response via de REST API (polling).
 * RIM-indieners (channel=RIM) ontvangen de FRM-response via Pulsar (AS4/eDelivery).
 *
 * Bij een Pulsar-fout wordt gelogd maar geen exception gegooid — de FRM is al opgeslagen.
 */
@Component
class FrmResponseEventListener(
    private val pulsarTemplate: PulsarTemplate<String>
) {

    private val logger = LoggerFactory.getLogger(FrmResponseEventListener::class.java)
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onFrmResponseCreated(event: FrmResponseCreatedEvent) {
        if (event.channel != SubmissionChannel.RIM) {
            // Webindieners hebben geen Pulsar-consumer — geen actie nodig
            logger.debug(
                "FrmResponseCreatedEvent overgeslagen (channel={}): formalityId={}",
                event.channel, event.formalityId
            )
            return
        }

        try {
            val message = objectMapper.writeValueAsString(event)
            pulsarTemplate.send(PulsarConfig.TOPIC_FRM_OUTBOUND, message)
            logger.info(
                "FrmResponseCreatedEvent gepubliceerd naar Pulsar: formalityId={}, status={}",
                event.formalityId, event.frmStatus
            )
        } catch (e: Exception) {
            // Log en slik de exception — de FRM is al opgeslagen in de database.
            logger.error(
                "Fout bij publiceren FrmResponseCreatedEvent naar Pulsar: formalityId={}, fout={}",
                event.formalityId, e.message, e
            )
        }
    }
}
