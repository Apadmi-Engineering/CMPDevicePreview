import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        namespace = "com.apadmi.cmpdevicepreview"
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

    // Everything but Android draws with Skia, and shares Compose's hooks for window insets and the
    // system theme.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                withJvm()
                withIos()
                withJs()
                withWasmJs()
            }
        }
    }

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
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            // Skiko's native runtime, which Compose's Path needs outside a window.
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui.test)
        }
    }
}

// Let the IDE resolve the generated catalog right after a Gradle sync.
tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
    dependsOn(generateDevicePresets)
}

// Publishing is signed only when the signing key is supplied (CI sets
// ORG_GRADLE_PROJECT_signingInMemoryKeyId and friends), so local builds and
// publishToMavenLocal need no key.
fun isSigningEnabled() = !System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId").isNullOrBlank()

mavenPublishing {

    coordinates("com.apadmi", "cmpdevicepreview", project.properties["devicePreviewVersion"] as String)
    pom {
        name = "CmpDevicePreview"
        description = "Kotlin & Compose Multiplatform library to simulate any device from any platform, supporting Android, iOS, JVM, JS, and WebAssembly."
        inceptionYear = "2026"
        url = "https://github.com/Apadmi-Engineering/CMPDevicePreview/"
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                name.set("Apadmi Ltd")
                url.set("https://github.com/Apadmi-Engineering")
            }
        }
        // The projects this one is built on: a fork of KMPDevicePreview, with its device catalog
        // (specs and frame artwork) taken from Flutter Device Preview.
        contributors {
            contributor {
                name.set("Mirzamehdi Karimov (KAppMaker)")
                url.set("https://github.com/KAppMaker/KMPDevicePreview")
            }
            contributor {
                name.set("Aloïs Deniel")
                url.set("https://github.com/aloisdeniel/flutter_device_preview")
            }
        }
        scm {
            connection.set("https://github.com/Apadmi-Engineering/CMPDevicePreview.git")
            url.set("https://github.com/Apadmi-Engineering/CMPDevicePreview")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/Apadmi-Engineering/CMPDevicePreview/issues")
        }
    }

    publishToMavenCentral()

    if (isSigningEnabled()) {
        signAllPublications()
    }
}
