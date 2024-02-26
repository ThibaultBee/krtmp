import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    `krtmp-publish`
}

description = "RTMP library"

kotlin {
    linuxX64()
    linuxArm64()

    macosX64()
    macosArm64()

    jvm()

    androidTarget {
        publishLibraryVariants("release", "debug")

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "rtmp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.network)
            api(libs.ktor.network.tls)
            api(libs.ktor.http)
            api(libs.ktor.client.core)
            api(libs.ktor.client.cio)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.coroutines.core)
            api(project(":flv"))
            api(project(":amf"))
            api(project(":common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    /**
     * Workaround for: Cannot locate tasks that match ':rtmp:testClasses' as task 'testClasses' not
     * found in project ':rtmp'. Some candidates are: 'jvmTestClasses'.
     */
    task("testClasses")
}

android {
    namespace = "io.github.thibaultbee.krtmp.rtmp"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
