# Contributing

## Prerequisites

- JDK 17 or newer (CI uses 17).
- The Android SDK, for the Android targets. Gradle finds it through the git-ignored `local.properties`.
- macOS with Xcode, for the iOS and macOS targets: Kotlin/Native only builds them there.

## Project layout

| Module | What it is |
| --- | --- |
| `cmpdevicepreview` | The library, published to Maven Central |
| `sample/composeApp` | The sample app's shared UI, and its web, iOS and macOS entry points |
| `sample/androidApp`, `sample/desktopApp` | The sample's Android and desktop entry points |
| `sample/iosApp` | The Xcode project that hosts the iOS entry point |

Everything under `sample/` is the sample app, and none of it is published.

In the library, platform code sits beside `commonMain`: `skikoMain` (JVM, iOS, macOS, JS and Wasm), `appleMain` (iOS and macOS) and `webMain` (JS and Wasm). `SimulatedPlatform` in `skikoMain` is the only file that uses Compose's internal hooks for window insets and the system theme, so a Compose update that changes them only breaks that file. Keep it that way.

The library uses `explicitApi()`.

## The sample app

`sample/composeApp` is a responsive sample app, and every platform's entry point shows it inside the preview, to prove the library runs on all of them:

| Platform | Entry point | Run it with |
| --- | --- | --- |
| Web (Wasm) | `sample/composeApp` (`webMain`) | `./gradlew :sample:composeApp:wasmJsBrowserDevelopmentRun`, or the [live demo](https://apadmi-engineering.github.io/CMPDevicePreview/) |
| Web (JS) | `sample/composeApp` (`webMain`) | `./gradlew :sample:composeApp:jsBrowserDevelopmentRun` |
| Desktop (JVM) | `sample/desktopApp` | `./gradlew :sample:desktopApp:run` |
| Android | `sample/androidApp` | `./gradlew :sample:androidApp:installDebug` |
| iOS | `sample/composeApp` (`iosMain`), hosted by `sample/iosApp` | open `sample/iosApp/iosApp.xcodeproj` in Xcode and run it |
| macOS (Apple silicon) | `sample/composeApp` (`macosMain`) | `./gradlew :sample:composeApp:runDebugExecutableMacosArm64` |

The desktop sample is also the quickest way to work on the sample app, or on the library, because it supports [Compose Hot Reload](https://github.com/JetBrains/compose-hot-reload): `./gradlew :sample:desktopApp:hotRun --auto` recompiles and reloads the running window as you edit, in `sample/composeApp` or `cmpdevicepreview`.

### What differs by platform

- **Android**: the sample keeps the preview inside the real system bars, to avoid showing them twice. The library can't simulate window insets or dark mode there (see [Platform differences](README.md#platform-differences)).
- **iOS**: the sample's preview sits inside the real safe area.
- **macOS** is Kotlin/Native, and Compose still marks it experimental, so `gradle.properties` sets `org.jetbrains.compose.experimental.macos.enabled`. Compose's native cursors are limited, so the resize handles show the arrow there.
- **Screenshots** of the device are saved from the web build only.
- **Mouse as touch** is web only, so on desktop lists scroll with the wheel, not by dragging.

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

### Code style

[Spotless](https://github.com/diffplug/spotless) runs [Diktat](https://github.com/saveourtool/diktat) over the Kotlin sources, configured by [`diktat-analysis.yml`](diktat-analysis.yml). `buildSrc` and the `.kts` build scripts aren't checked.

```bash
./gradlew spotlessCheck   # report violations
./gradlew spotlessApply   # fix the ones Diktat can fix
```

The `Lint` job in `pull_request.yml` runs `spotlessCheck`, and so does the pre-commit hook described under [Commit messages](#commit-messages). When you add a source set, add it to `srcDirectories` (or `testDirs` for a test source set) in `diktat-analysis.yml`.

`spotlessApply` rewrites files in place, and Diktat's autofix can rename a declaration without updating its uses, so review the diff and build before committing. Rules that clash with the code's conventions are disabled in `diktat-analysis.yml`, each with a reason.

## Adding or updating a device

The built-in devices are generated from JSON specs. [`cmpdevicepreview/device_specs/README.md`](cmpdevicepreview/device_specs/README.md) describes the format and how to add a device.

## Commit messages

Releases are automated. [release-please](https://github.com/googleapis/release-please) reads the commit messages that land on `main` to build the changelog and choose the next version, so each one follows [Conventional Commits](https://www.conventionalcommits.org): `type(scope): description`.

| Type | In the changelog |
|---|---|
| `feat`, `fix`, `perf`, `revert` | yes |
| `docs`, `refactor`, `build`, `ci`, `test`, `style`, `chore` | no, and on their own they don't trigger a release |

Breaking changes (`!` after the type, or a `BREAKING CHANGE:` footer) are always in the changelog.

Install the git hooks once per clone. The `commit-msg` hook rejects a message that isn't in this format, and the `pre-commit` hook runs `spotlessCheck` when you commit a Kotlin file (see [Code style](#code-style)):

```bash
git config core.hooksPath .githooks
```

When a pull request is squash-merged, its title becomes the commit message on `main`, so write the title in the same format. The hook can't check it.

## Releasing

For maintainers. The version is in `version.txt`. Don't edit it by hand: release-please owns it and the changelog.

1. release-please keeps a pull request open on `main` titled `chore(main): release <version>`, listing what has changed since the last release.
2. Merging that pull request tags the release and creates the GitHub release. The tag starts [`deploy.yml`](.github/workflows/deploy.yml), which runs the tests and publishes to Maven Central. Once that succeeds, it calls [`demo.yml`](.github/workflows/demo.yml), which builds the web sample and deploys it to GitHub Pages, the live demo the README links to. Each workflow lists the secrets it needs at the top.
3. Until 1.0, a breaking change bumps the minor version (`0.0.1` to `0.1.0`) and a `feat` or `fix` bumps the patch (`0.1.0` to `0.1.1`). Releases are marked as pre-releases on GitHub. To force a particular version, such as `1.0.0`, add a `Release-As: <version>` footer to a commit message.

If a publish fails after the release was created, re-run the failed **Deploy** run from the Actions tab. To redeploy the demo without a release, run the **Demo** workflow.

The demo needs two one-time repository settings, which need admin rights: **Settings > Pages > Source** set to **GitHub Actions**, and a `v*` tag rule under **Settings > Environments > github-pages > Deployment branches and tags**. Without the tag rule, a release tag's deployment is rejected.

## Dependency updates

Renovate runs on the first of each month and opens at most three pull requests: Compose, Kotlin and all other non-breaking updates. A major update gets its own. GitHub Actions are pinned to a commit SHA, and Renovate keeps those up to date and pins any new action you add by tag. Run the **Renovate** workflow from the Actions tab to get updates sooner, or to have it act on a rebase request.
