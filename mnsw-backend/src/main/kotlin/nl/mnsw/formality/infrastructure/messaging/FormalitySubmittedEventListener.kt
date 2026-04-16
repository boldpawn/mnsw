package nl.mnsw.formality.infrastructure.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.mnsw.config.PulsarConfig
import nl.mnsw.formality.application.FormalitySubmittedEvent
import org.slf4j.LoggerFactory
import org.springframework.pulsar.core.PulsarTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

/**
 * Luistert op FormalitySubmittedEvent en publiceert het naar het Pulsar-topic
 * mnsw-formalities-submitted, after-commit.
 *
 * De formality is al opgeslagen in de database vóór dit event gepubliceerd wordt.
 * Bij een Pulsar-fout wordt gelogd maar geen exception gegooid — de formality blijft
 * correct opgeslagen. Herverwerking kan plaatsvinden via een reconciliatie-job (toekomstige fase).
 */
@Component
class FormalitySubmittedEventListener(
    private val pulsarTemplate: PulsarTemplate<String>
) {

    private val logger = LoggerFactory.getLogger(FormalitySubmittedEventListener::class.java)
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onFormalitySubmitted(event: FormalitySubmittedEvent) {
        try {
            val message = objectMapper.writeValueAsString(event)
            pulsarTemplate.send(PulsarConfig.TOPIC_FORMALITIES_SUBMITTED, message)
            logger.info(
                "FormalitySubmittedEvent gepubliceerd naar Pulsar: formalityId={}, type={}, channel={}",
                event.formalityId, event.type, event.channel
            )
        } catch (e: Exception) {
            // Log en slik de exception — de formality is al opgeslagen en moet niet worden teruggedraaid.
            // Toekomstige fase: outbox-patroon of reconciliatie-job voor herverwerking.
            logger.error(
                "Fout bij publiceren FormalitySubmittedEvent naar Pulsar: formalityId={}, fout={}",
                event.formalityId, e.message, e
            )
        }
    }
}
