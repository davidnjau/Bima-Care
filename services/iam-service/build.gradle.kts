plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor.plugin)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("care.bima.iam.ApplicationKt")
}

dependencies {
    implementation(project(":shared:service-commons"))
    implementation(libs.ktor.server.netty)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
