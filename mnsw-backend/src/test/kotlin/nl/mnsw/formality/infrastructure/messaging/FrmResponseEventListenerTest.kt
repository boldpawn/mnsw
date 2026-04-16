package nl.mnsw.formality.infrastructure.messaging

import nl.mnsw.config.PulsarConfig
import nl.mnsw.formality.application.FrmResponseCreatedEvent
import nl.mnsw.formality.domain.SubmissionChannel
import nl.mnsw.formality.infrastructure.persistence.FrmStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.pulsar.core.PulsarTemplate
import java.util.UUID

class FrmResponseEventListenerTest {

    @Suppress("UNCHECKED_CAST")
    private val pulsarTemplate: PulsarTemplate<String> = mock()

    private val listener = FrmResponseEventListener(pulsarTemplate)

    private val formalityId = UUID.randomUUID()

    private fun buildEvent(channel: SubmissionChannel) = FrmResponseCreatedEvent(
        formalityId = formalityId,
        frmStatus = FrmStatus.ACCEPTED,
        reasonCode = null,
        reasonDescription = null,
        channel = channel
    )

    @Test
    fun `should send FRM to TOPIC_FRM_OUTBOUND when channel is RIM`() {
        val event = buildEvent(SubmissionChannel.RIM)

        listener.onFrmResponseCreated(event)

        val topicCaptor = argumentCaptor<String>()
        val messageCaptor2 = argumentCaptor<String>()
        verify(pulsarTemplate).send(topicCaptor.capture(), messageCaptor2.capture())
        assertEquals(PulsarConfig.TOPIC_FRM_OUTBOUND, topicCaptor.firstValue)
    }

    @Test
    fun `should not send FRM when channel is WEB`() {
        val event = buildEvent(SubmissionChannel.WEB)

        listener.onFrmResponseCreated(event)

        verify(pulsarTemplate, never()).send(any<String>(), any<String>())
    }

    @Test
    fun `should include formalityId in serialized FRM message for RIM channel`() {
        val event = buildEvent(SubmissionChannel.RIM)

        listener.onFrmResponseCreated(event)

        val messageCaptor = argumentCaptor<String>()
        verify(pulsarTemplate).send(any(), messageCaptor.capture())
        val json = messageCaptor.firstValue
        assert(json.contains(formalityId.toString())) {
            "JSON moet formalityId bevatten: $json"
        }
    }

    @Test
    fun `should not throw exception when PulsarTemplate throws for RIM channel`() {
        val event = buildEvent(SubmissionChannel.RIM)
        doThrow(RuntimeException("Pulsar unreachable"))
            .whenever(pulsarTemplate).send(any<String>(), any<String>())

        // Geen exception — de FRM is al opgeslagen in de database
        listener.onFrmResponseCreated(event)
    }

    @Test
    fun `should send ACCEPTED status in message for RIM channel`() {
        val event = buildEvent(SubmissionChannel.RIM)

        listener.onFrmResponseCreated(event)

        val messageCaptor = argumentCaptor<String>()
        verify(pulsarTemplate).send(any(), messageCaptor.capture())
        val json = messageCaptor.firstValue
        assert(json.contains("ACCEPTED")) {
            "JSON moet frmStatus ACCEPTED bevatten: $json"
        }
    }
}
