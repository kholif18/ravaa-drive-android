name: kora-android
description: Reference for the Kora file-type icon system on Android — full-color PNGs ported from the web theme plus the Kotlin mime/extension mapping. Use when adding new file-type icons, extending the mapping, fixing wrong icons, or touching ui/theme/KoraIcons.kt or res/drawable-nodpi/kora_*.png. Triggers on "kora", "icon", "file icon", "wrong icon", "docx icon", "pdf icon".
---

# Kora Icons on Android

## Source of truth

- Upstream SVGs live in the **web repo**: `Ravaa-Drive/public/icons/kora/*.svg` (64×64, gradients).
- Mapping logic mirror: web `components/drive/kora-icon.tsx` (`getKoraFileIcon`) ↔ Android `ui/theme/KoraIcons.kt` (`forFile`). Keep both in sync when adding types.

## Adding a new type

1. Render the SVG to PNG (exact pixels beat vector conversion for gradients):
   ```bash
   cd app/src/main
   rsvg-convert -w 192 -h 192 <src>.svg -o res/drawable-nodpi/kora_<name>.png
   ```
2. Resource name rules (aapt is strict): lowercase, `[a-z0-9_]`, dots/dashes → `_`, `+` → `cpp` (e.g. `text-x-c++.svg` → `kora_text_x_cpp.png`).
3. Extend `KoraIcons.forFile(name, mimeType)` — check extension first, then mime contains (same order as web: images → video → audio → pdf → archives → office → odf → code → fallback `kora_text_x_generic`).
4. Thumbnails: `KoraIcons.isPreviewable(name, mime)` decides thumb attempt (image/video only); icon is always the Coil placeholder/error fallback.

## Usage

```kotlin
// List/grid row (files only; folders use R.drawable.kora_folder directly)
FileIcon(file, size = 36.dp, imageLoader = vm.imageLoader, thumbUrl = vm.thumbUrl(file.id).takeIf { ... })
// or bare icon:
Image(painterResource(KoraIcons.forFile(name, mime)), name, Modifier.size(36.dp))
```

## Gotchas

- `drawable-nodpi` = no density scaling (192px source is enough for 32–48dp icons, downscales cleanly).
- Launcher icons are separate (`mipmap-*` from `img/icon.png`, adaptive fg/bg) — do not mix with `kora_*`.
- After adding PNGs, no gradle change needed; rebuild to verify `R.drawable` resolves.
