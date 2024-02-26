plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version "1.9.21"
    `krtmp-publish`
}

description = "Common code for krtmp"

kotlin {
    linuxX64()
    linuxArm64()

    mingwX64()

    macosX64()
    macosArm64()

    jvm()

    androidTarget {
        publishLibraryVariants("release", "debug")

        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "common"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.github.thibaultbee.krtmp.common"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
