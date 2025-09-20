import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)

    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.detekt).apply(false)
    alias(libs.plugins.kover)
    alias(libs.plugins.nexusPublish)
}

allprojects {
    group = "io.github.thibaultbee.krtmp"
    version = "0.9.0"
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            documentedVisibilities.set(
                setOf(
                    Visibility.PUBLIC,
                    Visibility.PROTECTED
                )
            )

            sourceLink {
                val exampleDir = "https://github.com/ThibaultBee/krtmp/tree/main"

                localDirectory.set(rootProject.projectDir)
                remoteUrl.set(URL(exampleDir))
                remoteLineSuffix.set("#L")
            }
        }
    }

    kover {
        reports {
            filters {
                excludes {
                    annotatedBy("kotlinx.serialization.Serializable")
                }
            }
        }
    }
}

dependencies {
    kover(project(":amf"))
    kover(project(":flv"))
    kover(project(":rtmp"))
    kover(project(":common"))
}

kover {
    reports {
        verify {
            rule {
                minBound(35, CoverageUnit.LINE)

                // we allow lower branch coverage, because not all checks in the internal code lead to errors
                minBound(25, CoverageUnit.BRANCH)
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            stagingProfileId.set(System.getenv("OSSRH_STAGING_PROFILE_ID"))
        }
    }
}
