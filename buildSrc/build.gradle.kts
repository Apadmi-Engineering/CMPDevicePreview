plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // JsonSlurper, for reading the device specs.
    implementation(localGroovy())
}
