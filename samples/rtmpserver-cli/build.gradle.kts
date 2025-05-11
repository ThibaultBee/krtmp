plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("io.github.thibaultbee.krtmp.rtmpserver.cli.MainKt")
}

dependencies {
    implementation(project(":rtmp"))

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.clikt)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}