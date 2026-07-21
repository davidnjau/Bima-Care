package care.bima.gateway

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

data class UpstreamRoute(val prefix: String, val baseUrl: String)

fun Routing.gatewayRoutes(
    client: HttpClient,
    upstreams: List<UpstreamRoute>,
) {
    authenticate("keycloak") {
        upstreams.forEach { upstream ->
            route("${upstream.prefix}/{...}") {
                handle {
                    proxyRequest(call, client, upstream.baseUrl)
                }
            }
        }
    }
}

private val BODY_METHODS = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)

private suspend fun proxyRequest(
    call: ApplicationCall,
    client: HttpClient,
    baseUrl: String,
) {
    val targetUrl = "$baseUrl${call.request.uri}"
    val incomingAuth = call.request.headers["Authorization"]
    val incomingContentType = call.request.headers["Content-Type"]
    val requestBody = if (call.request.httpMethod in BODY_METHODS) call.receiveText() else null

    val response =
        client.request(targetUrl) {
            method = call.request.httpMethod
            incomingAuth?.let { header("Authorization", it) }
            incomingContentType?.let { contentType(ContentType.parse(it)) }
            requestBody?.let { setBody(it) }
        }

    call.respondText(
        text = response.bodyAsText(),
        contentType = response.headers["Content-Type"]?.let { ContentType.parse(it) },
        status = response.status,
    )
}
