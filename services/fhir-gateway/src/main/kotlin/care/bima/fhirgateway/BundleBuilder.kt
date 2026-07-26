package care.bima.fhirgateway

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object BundleBuilder {
    fun searchset(resourceJsons: List<String>): String {
        val entries =
            resourceJsons.map { resourceJson ->
                buildJsonObject { put("resource", Json.parseToJsonElement(resourceJson)) }
            }
        val bundle =
            buildJsonObject {
                put("resourceType", "Bundle")
                put("type", "searchset")
                put("total", entries.size)
                put("entry", JsonArray(entries))
            }
        return bundle.toString()
    }
}
