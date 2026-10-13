import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)

}

val generateDevicePresets = tasks.register<GenerateDevicePresetsTask>("generateDevicePresets") {
    group = "build"
    description = "Generates the DevicePresets catalog from device_specs/*.json."
    specFiles.from(fileTree("device_specs") { include("*.json") })
    outputDirectory.set(layout.buildDirectory.dir("generated/devicePresets/kotlin"))
}

kotlin {
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.apadmi.cmpdevicepreview"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    // The browser tests need an executable binary, so webpack bundles the Skiko runtime (CMP-4906).
    js {
        browser {
            testTask { useKarma { useChromeHeadless() } }
        }
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask { useKarma { useChromeHeadless() } }
        }
        binaries.executable()
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
        }
        webTest.dependencies {
            implementation(kotlin("test"))
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

// The version lives in version.txt, which release-please rewrites in each release PR.
val libraryVersion = providers
    .fileContents(rootProject.layout.projectDirectory.file("version.txt"))
    .asText
    .map { it.trim() }

mavenPublishing {

    coordinates("com.apadmi", "cmpdevicepreview", libraryVersion.get())
    pom {
        name = "CmpDevicePreview"
        description = "Compose Multiplatform library that previews your app inside device frames and settings, on Android, iOS, desktop and the web."
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
            system.set("GitHub")
            url.set("https://github.com/Apadmi-Engineering/CMPDevicePreview/issues")
        }
    }

    publishToMavenCentral()

    if (isSigningEnabled()) {
        signAllPublications()
    }
}
