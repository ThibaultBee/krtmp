plugins {
    `maven-publish`
    signing
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven(url = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/") {
            name = "centralPortal"
            credentials {
                username = project.loadProperty("centralPortalUsername")
                password = project.loadProperty("centralPortalPassword")
            }
        }
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            name = "centralPortalSnapshots"
            credentials {
                username = project.loadProperty("centralPortalUsername")
                password = project.loadProperty("centralPortalPassword")
            }
        }
    }

    publications {
        create("mavenKotlin", MavenPublication::class.java) {
            from(components.getByName("kotlin"))
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(javadocJar.get())

        // Provide artifacts information requited by Maven Central
        pom {
            name.set(project.name)
            description.set("${project.description}")
            url.set("https://github.com/ThibaultBee/krtmp")

            licenses {
                license {
                    name = "Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    distribution = "repo"
                }
            }
            developers {
                developer {
                    name = "Thibault B."
                    organizationUrl = "https://github.com/ThibaultBee"
                }
            }
            scm {
                url.set("https://github.com/ThibaultBee/krtmp.git")
            }
        }
    }
}

if (project.hasProperty("signing_key_id") && project.hasProperty("signing_key") && project.hasProperty(
        "signing_password"
    )
) {
    signing {
        useInMemoryPgpKeys(
            project.loadProperty("signingInMemoryKeyId"),
            project.loadFileContents("signingInMemoryKey"),
            project.loadProperty("signingInMemoryKeyPassword")
        )
        sign(publishing.publications)
    }
}
