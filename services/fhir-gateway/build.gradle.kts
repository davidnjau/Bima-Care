plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor.plugin)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("care.bima.fhirgateway.ApplicationKt")
}

dependencies {
    implementation(project(":shared:service-commons"))
    implementation(project(":shared:fhir-models"))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.cio)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
