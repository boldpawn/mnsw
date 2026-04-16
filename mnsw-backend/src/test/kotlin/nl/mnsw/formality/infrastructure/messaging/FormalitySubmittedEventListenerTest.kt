package nl.mnsw.formality.infrastructure.messaging

import nl.mnsw.config.PulsarConfig
import nl.mnsw.formality.application.FormalitySubmittedEvent
import nl.mnsw.formality.domain.FormalityType
import nl.mnsw.formality.domain.SubmissionChannel
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.pulsar.core.PulsarTemplate
import java.time.OffsetDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.doThrow

class FormalitySubmittedEventListenerTest {

    @Suppress("UNCHECKED_CAST")
    private val pulsarTemplate: PulsarTemplate<String> = mock()

    private val listener = FormalitySubmittedEventListener(pulsarTemplate)

    private val formalityId = UUID.randomUUID()
    private val visitId = UUID.randomUUID()
    private val submitterId = UUID.randomUUID()

    private fun buildEvent(channel: SubmissionChannel = SubmissionChannel.WEB) = FormalitySubmittedEvent(
        formalityId = formalityId,
        visitId = visitId,
        type = FormalityType.NOA,
        portLocode = "NLRTM",
        submittedAt = OffsetDateTime.parse("2025-12-01T10:00:00Z"),
        channel = channel,
        submitterId = submitterId,
        messageIdentifier = "MSG-001"
    )

    @Test
    fun `should serialize event and send to TOPIC_FORMALITIES_SUBMITTED`() {
        val event = buildEvent()

        listener.onFormalitySubmitted(event)

        val topicCaptor = argumentCaptor<String>()
        val messageCaptor = argumentCaptor<String>()
        verify(pulsarTemplate).send(topicCaptor.capture(), messageCaptor.capture())

        assertEquals(PulsarConfig.TOPIC_FORMALITIES_SUBMITTED, topicCaptor.firstValue)
    }

    @Test
    fun `should include formalityId in serialized message`() {
        val event = buildEvent()

        listener.onFormalitySubmitted(event)

        val messageCaptor = argumentCaptor<String>()
        verify(pulsarTemplate).send(any(), messageCaptor.capture())

        val json = messageCaptor.firstValue
        assertTrue(json.contains(formalityId.toString()), "JSON moet formalityId bevatten: $json")
    }

    @Test
    fun `should include type and channel in serialized message`() {
        val event = buildEvent(channel = SubmissionChannel.RIM)

        listener.onFormalitySubmitted(event)

        val messageCaptor = argumentCaptor<String>()
        verify(pulsarTemplate).send(any(), messageCaptor.capture())

        val json = messageCaptor.firstValue
        assertTrue(json.contains("NOA"), "JSON moet type NOA bevatten: $json")
        assertTrue(json.contains("RIM"), "JSON moet channel RIM bevatten: $json")
    }

    @Test
    fun `should not throw exception when PulsarTemplate throws`() {
        val event = buildEvent()
        doThrow(RuntimeException("Pulsar connection failed"))
            .whenever(pulsarTemplate).send(any<String>(), any<String>())

        // Geen exception — de formality is al opgeslagen
        listener.onFormalitySubmitted(event)
    }
}
