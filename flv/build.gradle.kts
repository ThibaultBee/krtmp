import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `krtmp-publish`
}

description = "FLV muxer/demuxer library"

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
            baseName = "flv"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.coroutines.core)
            api(project(":amf"))
            api(project(":common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.jcodec)
        }
    }

    /**
     * Workaround for: Cannot locate tasks that match ':flv:testClasses' as task 'testClasses' not
     * found in project ':flv'. Some candidates are: 'jvmTestClasses'.
     */
    task("testClasses")
}

android {
    namespace = "io.github.thibaultbee.krtmp.flv"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
