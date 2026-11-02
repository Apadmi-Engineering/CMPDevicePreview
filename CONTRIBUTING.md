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

In the library, platform code sits beside `commonMain`: `skikoMain` (JVM, iOS, macOS, JS and Wasm), `appleMain` (iOS and macOS) and `webMain` (JS and Wasm). Three files use Compose's internals, listed in [Compose internals](#compose-internals): two in `skikoMain` and `LanguageEnvironment.kt` in `commonMain`. Keep it that way.

The library uses `explicitApi()`.

## Compose internals

Some of what the preview simulates has no public API in Compose Multiplatform, so the library uses Compose's internals for it. They are in `SimulatedPlatform.skiko.kt`, `SimulatedScreenLayers.skiko.kt` and, for Compose Resources, `LanguageEnvironment.kt`, so a Compose update that changes them only breaks those three files. Put any new use in one of them, and add it here. Android uses none of Compose's own: it has no hooks for any of this. Compose Resources' are in common code, so Android uses those.

| API | Marked | Used in | For |
| --- | --- | --- | --- |
| `LocalComposeSceneContext` | `internal`, reached with `@Suppress("INVISIBLE_REFERENCE")` | `SimulatedScreenLayers` | Handing `Dialog` and `Popup` layers that keep them to the device's screen |
| `ComposeSceneContext`, `ComposeSceneLayer`, `PlatformContext` | `@InternalComposeUiApi` | `SimulatedScreenLayers` | Wrapping the layers Compose makes for them |
| `LocalPlatformWindowInsets`, `PlatformWindowInsets`, `PlatformInsets`, `PlatformWindowInsetsProviderNode` | `@InternalComposeUiApi` | Both | The device's window insets, which `WindowInsets.safeDrawing`, `imePadding()` and the rest report |
| `LocalSystemTheme`, `SystemTheme` | `@InternalComposeUiApi` and deprecated, with `@Suppress("DEPRECATION")` | `SimulatedPlatform` | `isSystemInDarkTheme()` |
| `LocalComposeEnvironment`, `ComposeEnvironment`, and the constructor and properties of `ResourceEnvironment` | `internal` in Compose Resources, reached with `@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")` | `LanguageEnvironment` | The language `stringResource` and the rest of Compose Resources read inside the device |
| `LanguageQualifier`, `ScriptQualifier`, `RegionQualifier` | `@InternalResourceApi` | `LanguageEnvironment` | The language, script and region of that environment |

`LocalComposeSceneContext` is the riskiest. It is `internal`, not just marked as internal API, and the compiler warns that suppressing `INVISIBLE_REFERENCE` is unsupported and may stop working in a future Kotlin, which would show up as the library failing to build after a Kotlin update. An app can also use a newer Compose than the library was built with, so if the reference is missing when the app runs, the library catches the error and leaves dialogs and popups as Compose draws them, over the whole window. The comment above `findLocalSceneContext` explains how. `LocalComposeEnvironment` is handled the same way, by `findLocalResourceEnvironment`, and leaves the content in the host's language.

The layers also depend on how Compose 1.12's `Dialog` and `Popup` use them, which no API promises:

- A dialog sets its scrim colour before its content, and a popup never sets one. That is how a dialog's layer is told apart from a popup's.
- On iOS, a layer ignores the density it is given and provides the window's own density, window size and insets, so the library provides the device's again inside it.

A change to either breaks no build. A change to the first shows up as `SimulatedScreenLayersTest` failing, but the tests run on the JVM, so a change to the second only shows on iOS: open a dialog in the sample on iOS after a Compose update.

Experimental APIs, opted into with `@OptIn`, aren't listed here: they are public, if not yet stable.

When Compose or Kotlin is updated, check these before merging:

1. CI builds every target, which catches a removed or renamed API.
2. `./gradlew :cmpdevicepreview:jvmTest` runs `SimulatedScreenLayersTest` for dialogs and popups, `DeviceFrameViewTest` for insets and the theme, and `LanguageEnvironmentTest` for the language.
3. On iOS, open the dialog, sheet and menu on the sample's Profile tab, and check they stay on the device at its scale.
4. Check whether Compose now has a public API for any of these, and move to it if so.

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

The sample is localised with Compose Resources, in English (the default), Welsh and Portuguese, and offers them to the preview as `sampleLanguages`. To add a language, add a `values-<tag>` folder in `sample/composeApp/src/commonMain/composeResources` and an entry there. What reads the same as the English can be left out of it.

### What differs by platform

- **Android**: the sample keeps the preview inside the real system bars, to avoid showing them twice. The library can't simulate window insets, dark mode or dialogs there (see [Platform differences](README.md#platform-differences)).
- **iOS**: the sample's preview sits inside the real safe area.
- **macOS** is Kotlin/Native, and Compose still marks it experimental, so `gradle.properties` sets `org.jetbrains.compose.experimental.macos.enabled`. Compose's native cursors are limited, so the resize handles show the arrow there.
- **Dialogs, bottom sheets and menus**: the sample's Profile tab has one of each, to check they stay on the device's screen.
- **Screenshots** of the device are saved or copied from the web and desktop builds only. Desktop asks where to save with a native file dialog. The web can only copy from a secure context, so the menu has no Copy items on a page served over plain HTTP from another machine.
- **Mouse as touch** is web only, so on desktop lists scroll with the wheel, not by dragging, and the toolbar has no drag-to-scroll switch.

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

For maintainers. The version is in `version.txt`. Don't edit it by hand: release-please owns it and the changelog, and it also keeps the dependency line in the README's Getting Started section up to date, between the `x-release-please-start-version` and `x-release-please-end` comments.

1. release-please keeps a pull request open on `main` titled `chore(main): release <version>`, listing what has changed since the last release.
2. Merging that pull request tags the release and creates the GitHub release. The tag starts [`deploy.yml`](.github/workflows/deploy.yml), which runs the tests and publishes to Maven Central. Once that succeeds, it calls [`demo.yml`](.github/workflows/demo.yml), which builds the web sample and deploys it to GitHub Pages, the live demo the README links to. Each workflow lists the secrets it needs at the top.
3. Until 1.0, a breaking change bumps the minor version (`0.0.1` to `0.1.0`) and a `feat` or `fix` bumps the patch (`0.1.0` to `0.1.1`). Releases are marked as pre-releases on GitHub. To force a particular version, such as `1.0.0`, add a `Release-As: <version>` footer to a commit message.

If a publish fails after the release was created, re-run the failed **Deploy** run from the Actions tab. To redeploy the demo without a release, run the **Demo** workflow.

The demo needs two one-time repository settings, which need admin rights: **Settings > Pages > Source** set to **GitHub Actions**, and a `v*` tag rule under **Settings > Environments > github-pages > Deployment branches and tags**. Without the tag rule, a release tag's deployment is rejected.

## Dependency updates

Renovate runs on the first of each month and opens at most three pull requests: Compose, Kotlin and all other non-breaking updates. A major update gets its own. A Compose or Kotlin update needs the checks in [Compose internals](#compose-internals). GitHub Actions are pinned to a commit SHA, and Renovate keeps those up to date and pins any new action you add by tag. Run the **Renovate** workflow from the Actions tab to get updates sooner, or to have it act on a rebase request.
