plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        // The Kotlin plugins are only applied in the modules, so Spotless has no default target here.
        // buildSrc is left out on purpose: its task class is in the default package and Diktat would
        // add a package to it, which breaks the references to it from the build scripts.
        target(
            "cmpdevicepreview/src/**/*.kt",
            "composeApp/src/**/*.kt",
            "androidApp/src/**/*.kt",
            "desktopApp/src/**/*.kt",
        )
        targetExclude("**/build/**")

        diktat("1.2.5").configFile("diktat-analysis.yml")
    }
}
