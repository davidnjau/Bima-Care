pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "bima-care"

include(
    ":shared:events",
    ":shared:fhir-models",
    ":shared:service-commons",
    ":services:patient-service",
    ":services:provider-service",
    ":services:organization-service",
    ":services:eligibility-service",
    ":services:iam-service",
    ":gateway",
)
