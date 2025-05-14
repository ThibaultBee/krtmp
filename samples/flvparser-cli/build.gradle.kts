plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("io.github.thibaultbee.krtmp.flvparser.cli.MainKt")
}

dependencies {
    implementation(project(":flv"))

    implementation(libs.kotlinx.io.core)
    implementation(libs.clikt)
    
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}