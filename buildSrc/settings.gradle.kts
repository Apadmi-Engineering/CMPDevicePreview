dependencyResolutionManagement {
    versionCatalogs {
        // buildSrc is a build of its own, so it doesn't see the root build's catalog until told to.
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
