plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.ktor.server.core)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.postgresql)
    api(libs.hikaricp)
    api(libs.jwks.rsa)
    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
