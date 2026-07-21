package care.bima.shared.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.instance.model.api.IBaseResource

object FhirContextProvider {
    val r4: FhirContext by lazy { FhirContext.forR4() }

    private val jsonParser: IParser by lazy { r4.newJsonParser() }

    fun encodeToJson(resource: IBaseResource): String = jsonParser.encodeResourceToString(resource)

    fun <T : IBaseResource> decodeFromJson(
        json: String,
        type: Class<T>,
    ): T = jsonParser.parseResource(type, json)
}
