# CmpDevicePreview - Compose Multiplatform Device Preview Library

CmpDevicePreview is a Compose Multiplatform library for previewing your app on different devices. It draws your app inside a device frame and lets you change the device, orientation, dark mode, text size, display size, layout direction and keyboard from a panel, so you can see how it adapts without a real device or an emulator. It works on Android, iOS, desktop (JVM), JS and Wasm, and you can use it to put a web demo of your app online.

The device catalog, frames included, comes from [Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview). See [Credits](#credits).

## Getting Started
Add the library to your build.gradle file:
Latest version: [![Maven Central](https://img.shields.io/maven-central/v/com.apadmi/cmpdevicepreview?color=blue)](https://search.maven.org/search?q=g:com.apadmi).

```gradle
implementation("com.apadmi:cmpdevicepreview:<version>")
```

Wrap your root App composable, or any view, in `DeviceWithConfigurationView`. Your app needs nothing else from the library, so you can wrap it in a web-only entry point and leave the rest of your code untouched:
```kotlin
// wasmJsMain
fun main() {
    ComposeViewport(document.body!!) {
        DeviceWithConfigurationView {
            App()
        }
    }
}
```

Inside the device, Compose's own APIs report the simulated device, so your app doesn't need to know it is being previewed: window insets, `isSystemInDarkTheme()`, font scale, layout direction and window size classes all follow it. On Android only the window size does; insets and theme stay the real phone's.

The panel sits beside the device when there is room, and behind a button below it otherwise. Its settings, and your app's state, are kept when the window is resized. Turning preview mode off shows just your app until the page is reloaded.

Two features are web-only: the mouse acts as a finger on touchscreen devices, so dragging scrolls, and the panel can save screenshots as PNGs.

## Advanced Configuration

To start on a particular device or configuration:
```kotlin
DeviceWithConfigurationView(
    initialDevice = DevicePresets.iPhone16Pro,
    initialConfiguration = DeviceConfiguration(isDarkMode = true),
) {
    App()
}
```

To preview a device without the configuration panel:
```kotlin
SimulatedDevicePreview(
    simulatedDevice = SimulatedDevice(
        device = DevicePresets.iPhone16Pro, // A built-in device, or your own
        configuration = DeviceConfiguration(isDarkMode = true, isPortrait = true)
    )
) {
    App()
}
```

`DeviceFrameView(device = DevicePresets.galaxyS25) { App() }` draws a device around any content, with no panel and no state.

To use your own device, implement the `Device` interface (or create a `DevicePreset`) and pass it as `initialDevice`, or to `SimulatedDevicePreview` or `DeviceFrameView`. The panel's picker lists the built-in devices.

To add your own controls to the configuration panel, such as a button that resets your app's state or signs in a test user, pass `panelItems`. They are shown in order below the panel's settings:
```kotlin
DeviceWithConfigurationView(
    panelItems = listOf(
        { Button(onClick = { viewModel.reset() }) { Text("Reset app") } },
        { Button(onClick = { viewModel.signInTestUser() }) { Text("Sign in test user") } },
    ),
) {
    App()
}
```

## Sample

`composeApp` is a responsive sample app. Its web build shows it inside the preview: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`. On iOS it runs on its own, without the preview.

## Credits

**[Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview)** by Aloïs Deniel (MIT). The device frames, and the catalog of 33 devices behind them, are his work: the specs in [`cmpdevicepreview/device_specs`](cmpdevicepreview/device_specs) are copied unchanged and carry each device's screen size, safe areas, frame artwork, status bar artwork and keyboard heights. The SVG renderer that draws that artwork is ported from it, and so is the idea of previewing a running app on another device.

**[KMPDevicePreview](https://github.com/KAppMaker/KMPDevicePreview)** by KAppMaker (Mirzamehdi Karimov, MIT), which CmpDevicePreview is forked from. It was featured in [Android Weekly #650](https://androidweekly.net/issues/issue-650).

## License

[MIT](LICENSE). It keeps the copyright notices of Apadmi Ltd, KAppMaker and Aloïs Deniel.
