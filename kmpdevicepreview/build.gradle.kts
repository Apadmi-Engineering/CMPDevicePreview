//import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)

}

// Registers generateDevicePresets: device_specs/*.json -> DevicePresets.kt.
apply(from = "device-presets.gradle.kts")
val generateDevicePresets = tasks.named("generateDevicePresets")

kotlin {
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.cmpdevicepreview"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    js(IR) {
        nodejs()
        browser()
        binaries.library()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateDevicePresets)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.jetbrains.material3.windowssizeclass)
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            // Skiko's native runtime, which Compose's Path needs outside a window.
            implementation(compose.desktop.currentOs)
        }
    }
}

// Let the IDE resolve the generated catalog right after a Gradle sync.
tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
    dependsOn(generateDevicePresets)
}


mavenPublishing {

    coordinates("com.kappmaker", "kmpdevicepreview", project.properties["devicePreviewVersion"] as String)
    pom {
        name = "KMPDevicePreview"
        description = "Kotlin & Compose Multiplatform library to simulate any device from any platform, supporting Android, iOS, JVM, JS, and WebAssembly."
        inceptionYear = "2024"
        url = "https://github.com/KAppMaker/KMPDevicePreview/"
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                name.set("Mirzamehdi Karimov")
                email.set("mirzemehdi@gmail.com")
            }
        }
        scm {
            connection.set("https://github.com/KAppMaker/KMPDevicePreview.git")
            url.set("https://github.com/KAppMaker/KMPDevicePreview")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/KAppMaker/KMPDevicePreview/issues")
        }
    }

//    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
//    signAllPublications()
}
