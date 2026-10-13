# Device specs

One JSON file per device, `<id>.json`. These are copied unchanged from
[Flutter Device Preview](https://github.com/aloisdeniel/flutter_device_preview)'s
`device_specs/` (MIT, © Aloïs Deniel; see the repository's `LICENSE`). The metrics,
the frame artwork (screen outline and body), the status bar and gesture pill
artwork and the keyboard heights are all Aloïs Deniel's work, and they are the
source of the library's built-in catalog, `DevicePresets`.

The `generateDevicePresets` Gradle task (`GenerateDevicePresetsTask` in the root `buildSrc/`) turns
them into `DevicePresets.kt` under `build/generated/` on every build, so the
JSON is the only thing to edit. To update the catalog from Flutter, with both
repositories checked out side by side:

```console
cp ../../../flutter_device_preview/device_specs/*.json .
```

## What is used

| Key | Becomes |
|---|---|
| `id`, `name`, `brand`, `year` | `DevicePreset.id`, `name`, `brand`, `year` |
| `platform` | `DevicePreset.type`: `android` → `ANDROID`, `iOS` → `IOS`, anything else → `OTHER` |
| `kind` | `DevicePreset.kind` (`phone` when absent) |
| `portraitSize`, `devicePixelRatio` | The logical screen size and density |
| `portraitPadding`, `landscapePadding` | `portraitSafeArea`, `landscapeSafeArea` (landscape defaults to the rotation rule) |
| `frame` | `DeviceFrame`: `size`, `screenOffset`, `screenPath`, `body` |
| `systemUi` | `DevicePreset.systemUi`: the status bar and gesture pill artwork (its `platform` is ignored) |
| `portraitKeyboardHeight`, `landscapeKeyboardHeight` | The same names on `DevicePreset`, in logical pixels; no rotation rule between them |

`displayFeatures` (fold lines) are accepted and ignored: this library does not
simulate folds yet.

All lengths are logical pixels, described in portrait. The frame is drawn
rotated a quarter turn in landscape.

## `frame`

- `size` — the body, portrait.
- `screenOffset` — the screen's top-left corner inside the body.
- `screenPath` — SVG path data for the screen outline, in screen coordinates.
  The content is clipped to it with the non-zero fill rule, so cutouts are part
  of it: a notch indents the top edge, and a Dynamic Island or punch-hole is a
  second subpath wound against the outline.
- `body` — an SVG document drawn behind the screen, whose view box covers
  `size`. A string, or an array of lines joined with newlines.

The body renderer (`com.apadmi.cmpdevicepreview.svg`) supports the same subset
as Flutter's: `<svg>`, `<g>`, `<defs>`, `<clipPath>`, filled `<path>`,
`<rect>` (with `rx`/`ry`), `<circle>`, `<ellipse>` and `<polygon>`, `fill`,
`fill-opacity`, `fill-rule`, `opacity` (also inside `style="…"`), `transform`
and `clip-path="url(#id)"`. Strokes, gradients, filters, text, images and masks
are ignored.

## `systemUi`

```jsonc
"systemUi": {
  "statusBar": { "inset": 30, "leading": [/* clock SVG */], "trailing": [/* icons SVG */] },
  "navigationBar": { "center": [/* home indicator SVG */], "bottomInset": 8 }
}
```

Each bar fills the safe area on its side of the screen, and isn't drawn when
that side is 0 (an iPhone's status bar in landscape). `leading`, `center` and
`trailing` are SVG documents drawn at their view box size, never stretched:
`inset` is the distance from the screen edge to `leading` and `trailing`
(default 16), and `bottomInset` pins the artwork that far from the bar's outer
edge instead of centring it. Shapes with `fill="currentColor"` (or no fill) are
tinted to contrast with the simulated theme; use `fill-opacity` for secondary
detail.

## Adding a device

1. Copy the closest spec to `<brand>-<model>.json`, keeping `id` in sync with
   the file name.
2. Fix the metrics and redraw the body around the screen cut-out.
3. Build: the preset appears as `DevicePresets.<name>`, named from the id
   without its brand (`samsung-galaxy-s25` → `galaxyS25`).
4. Run `./gradlew :cmpdevicepreview:jvmTest`, which checks that every frame
   and status bar parses, that the screen outline matches the screen size, and
   that the status bar artwork fits its bars.
