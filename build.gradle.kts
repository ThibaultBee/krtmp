import kotlinx.kover.gradle.plugin.dsl.MetricType
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)

    alias(libs.plugins.dokka).apply(false)
    alias(libs.plugins.detekt).apply(false)
    alias(libs.plugins.kover)
    alias(libs.plugins.nexusPublish)
}

allprojects {
    group = "io.github.thibaultbee.krtmp"
    version = "1.0.0"
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

    koverReport {
        filters {
            excludes {
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
    }
}

dependencies {
    kover(project(":amf"))
    kover(project(":flv"))
    kover(project(":rtmp"))
}

koverReport {
    verify {
        rule {
            minBound(95, MetricType.LINE)

            // we allow lower branch coverage, because not all checks in the internal code lead to errors
            minBound(80, MetricType.BRANCH)
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
