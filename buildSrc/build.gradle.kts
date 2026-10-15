plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // JsonSlurper, for reading the device specs.
    implementation(localGroovy())
    // The Kotlin source writer for the generated device catalog.
    implementation(libs.kotlinpoet)
}
