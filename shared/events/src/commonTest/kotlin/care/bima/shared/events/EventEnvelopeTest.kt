package care.bima.shared.events

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class EventEnvelopeTest {
    @Test
    fun roundTripsThroughJson() {
        val envelope =
            EventEnvelope(
                eventId = "evt-1",
                eventType = Topics.PATIENT_CREATED,
                resourceId = "patient-1",
                version = 1,
                occurredAt = "2026-07-18T00:00:00Z",
                payload = "irrelevant-payload",
            )

        val serializer = EventEnvelope.serializer(String.serializer())
        val json = Json.encodeToString(serializer, envelope)
        val decoded = Json.decodeFromString(serializer, json)

        assertEquals(envelope, decoded)
    }
}
