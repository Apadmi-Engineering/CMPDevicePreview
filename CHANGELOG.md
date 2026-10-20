# Changelog

## [0.0.2](https://github.com/Apadmi-Engineering/CMPDevicePreview/compare/v0.0.1...v0.0.2) (2026-10-20)


### Features

* add reset buttons to the font scale slides and add ability to zoom into the device ([#23](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/23)) ([88e673d](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/88e673d6fd5eb31aa9e667a19a4a02cf339c1a75))

## 0.0.1 (2026-10-19)


### ⚠ BREAKING CHANGES

* through forked from KMPDevicePreview (see README.md), there is no backward compatbility and versioning restarts from 0.0.1.

### Features

* a responsive sample app, and window size classes in the preview ([d70ce57](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/d70ce57ddcd95a938f9b9618f62982355fe29789))
* add a "Built with CMPDevicePreview" link to the panel ([#20](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/20)) ([b3685d1](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/b3685d196851afde5a85009780becf0f25df7b3f))
* add a floating button to turn preview mode back on ([#17](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/17)) ([6a2f9da](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/6a2f9dabe1b3685dcf2b87acdd521ac278af3037))
* add macOS (Apple silicon) target ([#9](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/9)) ([c1fc9d9](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/c1fc9d947626aaa5b4a89539ee8b03eaa6e35409))
* add preview wrapper to non web target samples ([a23d165](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/a23d1650ba9eb99095bc9d2fc66f2f3614b586e1))
* all customisation of panel title and icon ([#19](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/19)) ([6affe2b](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/6affe2b7a63d390653ab7c6b3e3ca5f7b5de2f93))
* animate turning the device between portrait and landscape ([e1473f2](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/e1473f2f0dd3d323f64eaf3e67ca84530fa6b59d))
* chip hover and UI tweaks ([#16](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/16)) ([8b32c7a](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/8b32c7af8c4ac8b7365f37607c5da4ab06fc4092))
* freeform device, resized by dragging any corner ([09c6b53](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/09c6b536fd857d22dbfb6ebbcc397950a0ceee13))
* let app code use Compose's own insets and theme in the preview ([dc17447](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/dc17447943a5abaa6ab517a7c1e8543f42ef9d1b))
* let the caller add their own items to the configuration panel ([8c7748f](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/8c7748fa7162e7e03586b073e6f41c013adf63b4))
* let the mouse act as a finger on touchscreen devices ([012af8d](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/012af8d51666807036ee105879766bd8f401d051))
* port the Flutter device frames and sizes ([f038dfa](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/f038dfa55c8c697762738420f33e10ef98506c66))
* present devices as frames, with a note that nothing is simulated ([27cbab4](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/27cbab4807995587b6d8e916cc8149da6a0068bd))
* redesign the configuration panel and device picker ([4491c16](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/4491c16a1a6acd844a1e7894a6a2b9ae9adfd9ed))
* **sample:** add status bar background and icon controls ([1f68190](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/1f68190ec0cfaeffe15c139228db6262db970097))
* setup renovate and release-please ([#3](https://github.com/Apadmi-Engineering/CMPDevicePreview/issues/3)) ([c8fbc1c](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/c8fbc1c9f9daef562b567fe0863c4b64255c98a2))
* show the device on a dot grid, with a margin around it ([b51a77f](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/b51a77ff096919057fd7233a8a04be51c64e97bf))
* simulate the status bar and software keyboard ([2142e1c](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/2142e1c2fe9adfd9e93fc05e8cc1ccc810f699c7))
* start the preview on the Galaxy S25 ([c3ce8a2](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/c3ce8a2a335a41defa0b97df12792ad72aa1827c))
* text size, display size, layout direction, backdrop and screenshots in the panel ([9118411](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/9118411d929c1f7d7c448bfd8cd5a3345ce97c85))
* wire up release please and rennovate ([c8fbc1c](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/c8fbc1c9f9daef562b567fe0863c4b64255c98a2))


### Bug Fixes

* currentColor, screenshot file names and the signing check ([a38cc2b](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/a38cc2b7037ef8c8ddf9281c13230b6cdc5aee06))
* keep the preview's state when the window crosses the panel breakpoint ([8776f9e](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/8776f9ea6f31e48b9023c1332e5be68626ceb758))
* lock desktop windows to landscape ([9f563c0](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/9f563c00aaa7ebd59e3f9ef13a6cd101d28c0c13))
* **sample:** restore the installable Android app ([298a95a](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/298a95a3d6ef178d842f2c9e237701cf633277c6))
* stop a mouse drag on the simulated screen from clicking ([d1a7315](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/d1a73158e5c1392078596169423a4ef3cfb65c34))
* text colour in sample heading ([127277c](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/127277cfe885a4a2932e87aff66f6a00b4ff5cba))


### Code Refactoring

* describe devices in dp, with their kind and pixel ratio ([92200fc](https://github.com/Apadmi-Engineering/CMPDevicePreview/commit/92200fc7c57023f4c9964d0e9464db2b68000d8d))
