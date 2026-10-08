# KMPDevicePreview - Kotlin & Compose Multiplatform Preview Library

KMPDevicePreview is a Compose Multiplatform library for previewing and simulating your app. It lets you test your app or any part of it on different screen sizes, devices, and settings like dark mode or portrait/landscape orientation. This library works on Android, iOS, Web (WASM/Kotlin), JS and  Desktop (JVM). You can even use it to create web-based demos to showcase your app online.

[![Android Weekly badge](https://androidweekly.net/issues/issue-650/badge)](https://androidweekly.net/issues/issue-650)


## Getting Started
To use **KMPDevicePreview**, add the library to your build.gradle file:
Latest version: [![Maven Central](https://img.shields.io/maven-central/v/com.kappmaker/kmpdevicepreview?color=blue)](https://search.maven.org/search?q=g:com.kappmaker).

```gradle
implementation("com.kappmaker:kmpdevicepreview:<version>")

```
Wrap your root App composable or any view with `DeviceWithConfigurationView` to display a preview. Your app needs nothing else from the library, so you can wrap it in a web-only entry point and leave the rest of your code untouched:
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



https://github.com/user-attachments/assets/6393bd5b-a148-4903-a641-5d83bbc80d1d



## Advanced Configuration

### Devices and frames
The built-in catalog, `DevicePresets`, holds the same 33 devices as [Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview): current iPhones and iPads, Google Pixels, Samsung Galaxy phones, tablets and foldables, and two desktop windows. Each one comes with its screen size, pixel ratio, safe areas, frame artwork (bezel, rounded screen corners, notch, punch-hole or Dynamic Island), status bar and gesture pill, and keyboard height. The catalog is generated from the JSON specs in [`kmpdevicepreview/device_specs`](kmpdevicepreview/device_specs).

To preview only SimulatedDevice without any configuration view:
```kotlin

SimulatedDevicePreview(
    simulatedDevice = SimulatedDevice(
        device = DevicePresets.iPhone16Pro, // Use a predefined device or create your own
        configuration = DeviceConfiguration(
            isDarkMode = true,
            isPortrait = true,
            isFrameVisible = true,
            isSystemUiVisible = true, // Status bar and gesture pill
            isKeyboardVisible = false, // Simulated software keyboard
        )
    )
) {
    // Your composable content
    App()
}

```

`DeviceFrameView(device = DevicePresets.galaxyS25) { App() }` draws a device around any content, without the configuration panel. It takes the same `isPortrait`, `isFrameVisible`, `isSystemUiVisible`, `isKeyboardVisible` and `isDarkMode` flags. Desktop windows ignore `isPortrait`: they are resized rather than turned, so they always keep their natural, landscape shape (and the panel's Orientation switch is disabled for them).

You can create a custom device by implementing the `Device` interface (or creating a `DevicePreset`) and overriding its size properties, then pass it into `SimulatedDevice`. Give it a `DeviceFrame` to draw a body around it.

### Your app's code
Inside the simulated device, Compose's own APIs describe it, so your app doesn't need to know it's being previewed:
- `WindowInsets.safeDrawing`, `statusBars`, `navigationBars`, `displayCutout` and `ime`, the padding modifiers built on them (`safeDrawingPadding()`, `imePadding()`, …) and Material's defaults (Scaffold, TopAppBar) report the simulated device's notch, Dynamic Island, status bar, home indicator and keyboard.
- `isSystemInDarkTheme()` follows the **Appearance** switch.
- `LocalWindowInfo` reports the device's screen as the window, so window size classes (`currentWindowAdaptiveInfoV2()` and the breakpoints built on it) follow the simulated device rather than the browser window. The sample app switches between a bottom bar, a navigation rail and a navigation drawer this way.

```kotlin
MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding -> ... }
}
```

The insets and theme work on the web, desktop and iOS, through Compose Multiplatform hooks that are internal to Compose (one is deprecated), so a future Compose release may need a library update. Android has no such hooks: there, your app sees the real phone's insets and theme. The window size works everywhere.

### Status bar
Devices draw their status bar and gesture pill (or home indicator) over your content, in the safe area. They're static decoration: the clock never ticks, and the icons are white in dark mode and near-black in light mode. Turn them off with the **System UI** switch or `isSystemUiVisible = false`; the safe area stays the same either way, and the keyboard has its own switch.

### Keyboard
The **Keyboard** switch raises the device's software keyboard, at the height the real keyboard covers on that device (each iPhone and iPad measured; every Android device shares one representative height). Focusing a text field can't raise it: Compose text fields talk to the platform's keyboard directly, so the preview never hears about it.

While it's up, a translucent band covers the bottom of the screen, and `WindowInsets.ime` (and so `safeDrawing` and `imePadding()`) reports its height, so content that avoids the keyboard on a real phone avoids it here too.

### Mouse input
In a browser, the mouse acts as a finger on a phone, tablet or foldable: dragging the screen scrolls lists, as it would on the device (Compose ignores mouse drags otherwise). A press that starts on the screen is sent to your content as a touch until it's released. The wheel, hover and right-click work as usual, and a desktop window keeps the mouse as a mouse. Other hosts, such as a JVM desktop app, keep the mouse as it is.

####
This library is inspired by [Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview). It brings similar functionality to the Compose Multiplatform ecosystem.

