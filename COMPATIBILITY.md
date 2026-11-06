# Compatibility

Each release of CmpDevicePreview is built and tested with one version of Compose Multiplatform and of Kotlin:

| CmpDevicePreview | Compose Multiplatform | Kotlin |
| --- | --- | --- |
| 0.0.1 onwards | 1.12.2 | 2.4.21 |

A newer Compose is untested. The preview relies on parts of Compose that aren't public API, so a newer Compose can break dialogs, insets, the language or HTML elements inside the device. Use the release built with your Compose version where there is one.
