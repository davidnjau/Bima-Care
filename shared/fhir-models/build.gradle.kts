plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.hapifhir.structures.r4)
    implementation(libs.hapifhir.validation)

    testImplementation(kotlin("test"))
}
