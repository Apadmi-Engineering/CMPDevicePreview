# CLAUDE.md

CmpDevicePreview is a Compose Multiplatform library that draws an app inside a device frame, with a panel to change the device, orientation, dark mode, text and display size, layout direction and keyboard. [README.md](README.md) covers using it and [CONTRIBUTING.md](CONTRIBUTING.md) covers the project layout, running each target, releasing and the device specs. This file is only what isn't there.

## Comments

Write self-documenting code: names and structure say what it does, comments are rare.

- Don't write KDoc for `internal` or `private` declarations, such as composables, functions, properties and constants. Only the public API (the library builds with `explicitApi()`) is documented.
- A comment is for a non-obvious *why*, in one short `//` line. Don't describe what the code does, or what you just changed.
- Diktat only asks for `@param` and `@return` tags when a KDoc is present, and a missing KDoc is allowed, so leaving it out is lint-clean. When you change a function that already has a KDoc, keep its tags in sync with a one-line entry each, and don't expand the existing text.

## The library

- Code is in `cmpdevicepreview/src/commonMain/.../`: `core/` is the device and configuration model, `preview/` is the panel, picker, device view and resize handles, `frame/` draws frames and system UI, `svg/` renders the frame artwork, and `devices/` is the catalog.
- The public API is `DeviceWithConfigurationView`, `ConfigurationView`, `SimulatedDevicePreview`, `DeviceFrameView` and the model types in `core/`. Everything else is `internal`. Changing a public signature is a breaking change, so the commit needs `!`.
- `DevicePresets` is generated from `cmpdevicepreview/device_specs/*.json` on every build. Never edit the generated file: change the JSON, as `device_specs/README.md` describes.
- The preview's zoom and pan are a graphics transform held by `PreviewViewport`, not a public setting or a new layout, so the app isn't laid out again as you zoom. The wheel reports pixels on the web and notches elsewhere: `isWheelDeltaInPixels` accounts for it.
- A new field on `DeviceConfiguration` or `PreviewSettings` also needs its key in `PreviewSavers.kt`, so it survives a configuration change, with a case in `PreviewSaversTest`.

## The panel

- It is dark and styled by hand, not by Material's defaults. Take colours from `PanelColors` and reuse the components in `PanelTheme.kt` (`SegmentedControl`, `ToggleRow`, `StepSlider`, `PanelIconButton`, `PanelTooltip`, `SectionLabel`) rather than adding new ones. `ConfigurationView.kt` composes them.
- Every change to the device goes through `ConfigurationView`'s `update`, which fits it to the device with `adaptedToDevice()`.
- A setting with a default that the user can move away from shows a `ResetButton`, only while it differs from the default. The text and display size sliders and the layout direction do. The header's reset-all button does the same for the whole device configuration.
- Keep text at 4.5:1 contrast or better against the panel, and controls and their state at 3:1. Borders and the dot grid are decorative and exempt. Hover, focus and press feedback comes from the ripple set in `PanelTheme`.

## Tests

- They live in `cmpdevicepreview/src/jvmTest`, using Compose's UI test and `renderComposeScene`, and run with `./gradlew :cmpdevicepreview:jvmTest` (one class: `--tests '*ConfigurationViewTest*'`). `webTest` runs in headless Chrome through Karma.
- Put a test for new panel behaviour in `ConfigurationViewTest`, or the `*Test` named for what you changed. Test names read as sentences about behaviour, such as `aTypedSizeIsFittedToTheDevice`.
- To see how the panel looks without a browser, render it in a temporary `jvmTest` with `renderComposeScene` and write the image with Skia's `encodeToData`. Delete it afterwards: Spotless lints temporary tests too.

## Running it

- The web sample is the one to check the panel in. `./gradlew :sample:composeApp:wasmJsBrowserDevelopmentRun --continuous` serves it, on port 8080 unless that is taken. Without `--continuous` it builds once and ignores later edits. Reload the page after a rebuild.
- The desktop sample's hot reload is quicker for UI work: `./gradlew :sample:desktopApp:hotRun --auto`.

## Before committing

- Run `./gradlew spotlessCheck :cmpdevicepreview:jvmTest`. `spotlessApply` rewrites files and can rename a declaration without updating its uses, so review its diff.
- Commit messages are Conventional Commits (`feat:`, `fix:`, `chore:`, ...), which release-please reads for the changelog and the next version. Don't edit `version.txt` or the changelog by hand.
- The git hooks in `.githooks` only run once `git config core.hooksPath .githooks` has been set, which a fresh clone or worktree doesn't have.
