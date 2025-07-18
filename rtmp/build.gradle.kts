import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
                    jvmTarget.set(JvmTarget.JVM_18)
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
            implementation(libs.ktor.network.tls)
            implementation(libs.ktor.http)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
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
        androidMain {
            kotlin.srcDir("src/commonJvmAndroid/kotlin")
        }
        jvmMain {
            kotlin.srcDir("src/commonJvmAndroid/kotlin")
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
    compileSdk = 36
    defaultConfig {
        minSdk = 21
    }
}
