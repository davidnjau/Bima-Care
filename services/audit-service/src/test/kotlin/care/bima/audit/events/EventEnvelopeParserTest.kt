package care.bima.audit.events

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventEnvelopeParserTest {
    @Test
    fun parsesCommonEnvelopeFieldsAndKeepsPayloadRaw() {
        val eventId = UUID.randomUUID()
        val resourceId = UUID.randomUUID()
        val rawJson =
            """
            {"eventId":"$eventId","eventType":"claim.adjudicated","resourceId":"$resourceId",
            "version":1,"occurredAt":"2026-01-01T00:00:00Z",
            "payload":{"claimId":"$resourceId","status":"APPROVED","approvedAmount":"8500"}}
            """.trimIndent()

        val record = EventEnvelopeParser.parse(rawJson)

        assertEquals(eventId, record.id)
        assertEquals("claim.adjudicated", record.eventType)
        assertEquals(resourceId.toString(), record.resourceId)
        assertEquals(1, record.version)
        assertEquals("2026-01-01T00:00:00Z", record.occurredAt)
        assertEquals(
            """{"claimId":"$resourceId","status":"APPROVED","approvedAmount":"8500"}""",
            record.payload,
        )
    }
}
