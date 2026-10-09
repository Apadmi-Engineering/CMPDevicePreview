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

## Commit messages

Releases are automated. [release-please](https://github.com/googleapis/release-please) reads the commit messages that land on `main` to build the changelog and choose the next version, so each one follows [Conventional Commits](https://www.conventionalcommits.org): `type(scope): description`.

| Type | Use it for | In the changelog |
|---|---|---|
| `feat` | a new capability | yes |
| `fix` | a bug fix | yes |
| `perf`, `revert` | a performance improvement, a revert | yes |
| `docs`, `refactor`, `build`, `ci`, `test`, `style`, `chore` | everything else | no, and on their own they don't trigger a release |

Mark a breaking change with `!` after the type, as in `refactor!: describe devices in dp`, or with a `BREAKING CHANGE:` footer. Breaking changes are always in the changelog.

Install the commit-message check once per clone. It rejects a message that isn't in this format:

```bash
git config core.hooksPath .githooks
```

When a pull request is squash-merged, its title becomes the commit message on `main`, so write the title in the same format. The hook can't check it.

## Releasing

For maintainers. The version is in `version.txt`. Don't edit it by hand: release-please owns it and the changelog.

1. release-please keeps a pull request open on `main` titled `chore(main): release <version>`, listing what has changed since the last release.
2. Merging that pull request tags the release and creates the GitHub release. The tag starts [`deploy.yml`](.github/workflows/deploy.yml), which runs the tests and publishes to Maven Central. The repository secrets it needs are listed at the top of that file, and the one release-please needs is listed at the top of [`release-please.yml`](.github/workflows/release-please.yml).
3. Until 1.0, a breaking change bumps the minor version (`0.0.1` to `0.1.0`) and a `feat` or `fix` bumps the patch (`0.1.0` to `0.1.1`). Releases are marked as pre-releases on GitHub. To force a particular version, such as `1.0.0`, add a `Release-As: <version>` footer to a commit message.

If a publish fails after the release was created, re-run the failed **Deploy** run from the Actions tab.

## Dependency updates

Renovate runs on the first of each month and opens its pull requests in at most three groups: Compose, Kotlin, and all other non-breaking updates. A major update gets its own pull request. Run the **Renovate** workflow from the Actions tab to get updates sooner, or to have it act on a rebase request.
