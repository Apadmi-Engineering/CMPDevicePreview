# CmpDevicePreview - Compose Multiplatform Device Preview Library

CmpDevicePreview is a Compose Multiplatform library that draws your app inside a device frame, with a panel to change the device, orientation, dark mode, text size, display size, layout direction and keyboard. It runs on Android, iOS, macOS (Apple silicon), desktop (JVM), JS and Wasm, so you can put a web demo of your app online.

**[Try the live demo](https://apadmi-engineering.github.io/CMPDevicePreview/)**: the sample app running in the preview, in your browser.

[Demo-CMPDevicePreview.webm](https://github.com/user-attachments/assets/002719e9-7886-4bf6-b982-a2e65f1c7589)

## Getting Started
Add the library to your build.gradle file:
Latest version: [![Maven Central](https://img.shields.io/maven-central/v/com.apadmi/cmpdevicepreview?color=blue)](https://search.maven.org/search?q=g:com.apadmi).

```gradle
implementation("com.apadmi:cmpdevicepreview:<version>")
```

Wrap your root composable in `DeviceWithConfigurationView`. You can do this in a single platform's entry point, such as the web one, and leave the rest of your code untouched:
```kotlin
DeviceWithConfigurationView {
    App()
}
```

Inside the device, Compose's own APIs report the simulated device, so your app doesn't need to know it is being previewed: window insets, `isSystemInDarkTheme()`, font scale, layout direction and window size classes all follow it, and dialogs, bottom sheets and menus stay on its screen. Note: On Android platform limitations mean not all of these work correctly (see [Platform differences](#platform-differences)).

## Developing with Hot Reload

The desktop (JVM) target is the quickest way to work on your UI: run it with [Compose Hot Reload](https://github.com/JetBrains/compose-hot-reload) and edits show up in the running device frame without a restart. Make the window wide enough to show the panel beside the device:

```kotlin
// desktopMain
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(size = DpSize(1280.dp, 900.dp)),
    ) {
        DeviceWithConfigurationView {
            App()
        }
    }
}
```

## Advanced Configuration

To start on a particular device or configuration:
```kotlin
DeviceWithConfigurationView(
    initialDevice = DevicePresets.iPhone16Pro,
    initialConfiguration = DeviceConfiguration(appearance = Appearance.Dark),
) {
    App()
}
```

To preview a device without the configuration panel:
```kotlin
SimulatedDevicePreview(
    simulatedDevice = SimulatedDevice(
        device = DevicePresets.iPhone16Pro, // A built-in device, or your own
        configuration = DeviceConfiguration(appearance = Appearance.Dark, orientation = DeviceOrientation.Portrait)
    )
) {
    App()
}
```

`DeviceFrameView(device = DevicePresets.galaxyS25) { App() }` draws a device around any content, with no panel and no state.

To use your own device, implement the `Device` interface (or create a `DevicePreset`) and pass it as `initialDevice`, or to `SimulatedDevicePreview` or `DeviceFrameView`. The panel's picker lists the built-in devices.

## Saving settings and sharing links

The device and the panel's settings are kept across reloads and restarts. On the web they are also in the page's URL, so you can share a link to the same preview:

```
https://your.site/?dp.device=apple-ipad-pro-13-m4&dp.orientation=landscape&dp.appearance=dark
```

A link's settings win over the kept ones, and the panel's reset button clears both. Apps on the same browser origin share the kept settings: pass `isPersistenceEnabled = false` to `DeviceWithConfigurationView` to turn this off.

## Customisation

The configuration panel can carry your own branding and controls. Both are parameters of `DeviceWithConfigurationView`, and of `ConfigurationView` if you place the panel yourself.

### Header

The header shows "Device Preview" beside an icon. Pass `panelTitle` and `panelIcon` to replace them:
```kotlin
DeviceWithConfigurationView(
    panelTitle = "Acme Preview",
    panelIcon = {
        Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.fillMaxSize())
    },
) {
    App()
}
```

The icon fills a square 28 dp across and replaces the whole of the default one, so it brings any background of its own.

### Attribution

The end of the panel has a "Built with CMPDevicePreview" link to the project's GitHub repository. Pass `isAttributionVisible = false` to hide it:
```kotlin
DeviceWithConfigurationView(isAttributionVisible = false) {
    App()
}
```

### Your own controls

To add your own controls to the configuration panel, pass `panelItems`. They are shown in order below the panel's settings, as wide as them and inside the panel's dark Material theme:
```kotlin
DeviceWithConfigurationView(
    panelItems = listOf(
        { Button(onClick = { viewModel.reset() }) { Text("Reset app") } },
    ),
) {
    App()
}
```

### Languages

To let the panel switch the language of your app, pass `languages`. The first is the default, and without any the panel has no language setting:
```kotlin
DeviceWithConfigurationView(
    languages = listOf(
        PreviewLanguage("en", "English"),
        PreviewLanguage("cy", "Cymraeg"),
    ),
) {
    App()
}
```

Inside the device, `stringResource` and the rest of [Compose Resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html) read the chosen language, from your `values-cy` folder and so on.

## Platform differences

- **Android** can't override the window insets or the dark mode its host reports, so the app inside the device sees the real ones. Its dialogs, bottom sheets and menus are windows of their own, so they aren't kept to the device's screen.
- **Languages** only change Compose Resources: Android's own `R.string` and what the system localises follow the host's language.
- **Screenshots** of the device are saved from the web build only, and leave out an open dialog, bottom sheet or menu.

## Credits

**[Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview)** by Aloïs Deniel (MIT). The device frames and the catalogue devices behind them are his work, copied into [`cmpdevicepreview/device_specs`](cmpdevicepreview/device_specs) with minor modifications. The SVG renderer that draws them is ported from it.

**[KMPDevicePreview](https://github.com/KAppMaker/KMPDevicePreview)** by KAppMaker (Mirzamehdi Karimov, MIT), which CmpDevicePreview is forked from.

## Licence

[MIT](LICENSE). It keeps the copyright notices of Apadmi Ltd, KAppMaker and Aloïs Deniel.
