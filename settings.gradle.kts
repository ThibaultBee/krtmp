enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "krtmp"
include(":rtmp")
include(":flv")
include(":amf")
include(":common")
include("samples:flvparser-cli")
include("samples:rtmpserver-cli")
include("samples:rtmpclient-cli")
