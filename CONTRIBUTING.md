# Contributing

## Prerequisites

- JDK 17 or newer (CI uses 17).
- The Android SDK, for the Android targets. Android Studio creates the git-ignored `local.properties` that points Gradle at it.
- macOS with Xcode, for the iOS targets: Kotlin/Native only builds them there, which is why CI builds iOS on a macOS runner.

## Project layout

| Module | What it is |
| --- | --- |
| `cmpdevicepreview` | The library, published to Maven Central |
| `composeApp` | The sample app's shared UI, and its web and iOS entry points |
| `androidApp`, `desktopApp` | The sample's Android and desktop entry points |
| `iosApp` | The Xcode project that hosts the iOS entry point |

In the library, platform code sits beside `commonMain`. Everything but Android draws with Skia, so JVM, iOS, JS and Wasm share `skikoMain`, and JS and Wasm share `webMain`. `SimulatedPlatform` in `skikoMain` is the only file that uses Compose's internal hooks for window insets and the system theme, so that a Compose update that changes them only breaks that file. Keep it that way.

The library builds with `explicitApi()`, so every public declaration needs an explicit visibility and type.

## The sample app

`composeApp` is a responsive sample app, and every platform's entry point shows it inside the preview, to prove the library runs on all of them:

| Platform | Entry point | Run it with |
| --- | --- | --- |
| Web | `composeApp` (`wasmJsMain`) | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| Desktop (JVM) | `desktopApp` | `./gradlew :desktopApp:run` |
| Android | `androidApp` | `./gradlew :androidApp:installDebug` |
| iOS | `composeApp` (`iosMain`), hosted by `iosApp` | open `iosApp/iosApp.xcodeproj` in Xcode and run it |

The desktop sample is also the quickest way to work on the sample app, or on the library, because it supports [Compose Hot Reload](https://github.com/JetBrains/compose-hot-reload): `./gradlew :desktopApp:hotRun --auto` recompiles and reloads the running window as you edit, in `composeApp` or `cmpdevicepreview`. Hot Reload runs on a JetBrains Runtime, which Gradle downloads the first time if there isn't one installed.

### What differs by platform

- **Android** can't override the window insets or the dark mode its host reports, so the app inside the device sees the real ones, not the simulated device's. The sample keeps the preview inside the real system bars to avoid showing them twice. The iOS, desktop and web previews simulate both.
- **iOS**: the sample's preview sits inside the real safe area.
- **Screenshots** of the device are saved from the web build only, and only there does the mouse act as a finger, so on desktop lists scroll with the wheel, not by dragging.

## Tests and checks

The library's tests run on the JVM, with Compose's UI test support rendering the frame and the panel:

```bash
./gradlew :cmpdevicepreview:jvmTest
```

A pull request is built once per target by [`pull_request.yml`](.github/workflows/pull_request.yml). To reproduce one locally, run the Gradle tasks listed in the matching entry of its `matrix`.

To try your changes in another project, publish the library to your local Maven repository and add `mavenLocal()` to that project's repositories:

```bash
./gradlew :cmpdevicepreview:publishToMavenLocal
```

## Adding or updating a device

The built-in devices are generated from JSON specs. [`cmpdevicepreview/device_specs/README.md`](cmpdevicepreview/device_specs/README.md) describes the format, how to add a device, and how to update the catalog from Flutter Device Preview, where the specs come from.

## Releasing

For maintainers. Set `devicePreviewVersion` in `gradle.properties`, then push a tag that starts with `v`. [`deploy.yml`](.github/workflows/deploy.yml) then runs the tests, publishes to Maven Central and creates a GitHub release. The repository secrets it needs are listed at the top of that file.
