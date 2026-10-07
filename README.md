# KMPDevicePreview - Kotlin & Compose Multiplatform Preview Library

KMPDevicePreview is a Compose Multiplatform library for previewing and simulating your app. It lets you test your app or any part of it on different screen sizes, devices, and settings like dark mode or portrait/landscape orientation. This library works on Android, iOS, Web (WASM/Kotlin), JS and  Desktop (JVM). You can even use it to create web-based demos to showcase your app online.

[![Android Weekly badge](https://androidweekly.net/issues/issue-650/badge)](https://androidweekly.net/issues/issue-650)


## Getting Started
To use **KMPDevicePreview**, add the library to your build.gradle file:
Latest version: [![Maven Central](https://img.shields.io/maven-central/v/com.kappmaker/kmpdevicepreview?color=blue)](https://search.maven.org/search?q=g:com.kappmaker).

```gradle
implementation("com.kappmaker:kmpdevicepreview:<version>")

```
Wrap your root App composable or any view with `DeviceWithConfigurationView` to display a preview:
```kotlin
DeviceWithConfigurationView {
    App()
}
```



https://github.com/user-attachments/assets/6393bd5b-a148-4903-a641-5d83bbc80d1d



## Advanced Configuration

### Devices and frames
The built-in catalog, `DevicePresets`, holds the same 33 devices as [Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview): current iPhones and iPads, Google Pixels, Samsung Galaxy phones, tablets and foldables, and two desktop windows. Each one comes with its screen size, pixel ratio, safe areas and frame artwork (bezel, rounded screen corners, notch, punch-hole or Dynamic Island). The catalog is generated from the JSON specs in [`kmpdevicepreview/device_specs`](kmpdevicepreview/device_specs).

To preview only SimulatedDevice without any configuration view:
```kotlin

SimulatedDevicePreview(
    simulatedDevice = SimulatedDevice(
        device = DevicePresets.iPhone16Pro, // Use a predefined device or create your own
        configuration = DeviceConfiguration(isDarkMode = true, isPortrait = true, isFrameVisible = true) // Set dark mode, orientation and frame
    )
) {
    // Your composable content
    App()
}

```

`DeviceFrameView(device = DevicePresets.galaxyS25) { App() }` draws a device around any content, without the simulated theme.

You can create a custom device by implementing the `Device` interface (or creating a `DevicePreset`) and overriding its size properties, then pass it into `SimulatedDevice`. Give it a `DeviceFrame` to draw a body around it.

### Safe area
Use `SimulatedDeviceSafeArea.current` to keep content clear of the simulated device's notch, Dynamic Island, status bar and home indicator:

```kotlin
LazyColumn(contentPadding = SimulatedDeviceSafeArea.current.asPaddingValues()) {
    ...
}
```

When the preview mode is turned off, `DeviceWithConfigurationView` provides the real device's `WindowInsets.safeDrawing` instead.

### Theme Configuration
Use `SimulatedDeviceThemeIsDark.current` for testing light/dark mode previews:

```kotlin
val isDark by SimulatedDeviceThemeIsDark.current
MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()){
    ...
}


```
####
This library is inspired by [Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview). It brings similar functionality to the Compose Multiplatform ecosystem.

