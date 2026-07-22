plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor.plugin)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("care.bima.payments.ApplicationKt")
}

dependencies {
    implementation(project(":shared:service-commons"))
    implementation(project(":shared:events"))
    implementation(project(":shared:fhir-models"))
    implementation(libs.ktor.server.netty)
    implementation(libs.exposed.java.time)
    implementation(libs.kafka.clients)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
