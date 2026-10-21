import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.apadmi.cmpdevicepreview.sample"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // The desktop app that runs this is in :sample:desktopApp.
    jvm()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "com.apadmi.cmpdevicepreview.sample.main"
        }
    }

    val rootDirPath = project.rootDir.path
    val projectDirPath = project.projectDir.path
    val siteWebpackConfig = Action<KotlinWebpackConfig> {
        outputFileName = "composeApp.js"
        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
            static(rootDirPath)
            static(projectDirPath)
        }
    }

    js {
        browser {
            commonWebpackConfig(siteWebpackConfig)
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig(siteWebpackConfig)
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.jetbrains.material3.adaptive)
        }
        // The entry points wrap the app in the preview (Android and desktop (JVM): :sample:androidApp, :sample:desktopApp).
        iosMain.dependencies {
            implementation(project(":cmpdevicepreview"))
        }
        macosMain.dependencies {
            implementation(project(":cmpdevicepreview"))
        }
        webMain.dependencies {
            implementation(project(":cmpdevicepreview"))
        }
    }
}

compose.resources {
    packageOfResClass = "com.apadmi.cmpdevicepreview.sample.resources"
    publicResClass = false
}
