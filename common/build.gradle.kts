import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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

    /**
     * Workaround for: Cannot locate tasks that match ':rtmp:testClasses' as task 'testClasses' not
     * found in project ':rtmp'. Some candidates are: 'jvmTestClasses'.
     */
    task("testClasses")
}

android {
    namespace = "io.github.thibaultbee.krtmp.common"
    compileSdk = 36
    defaultConfig {
        minSdk = 21
    }
}
