# TinyVid - Video Compressor - Build Notes

**Built from:** n9nik/tinypic-compressor @ a972598 (green-build-proven base)
**Date:** Sep 10 2026
**Package:** com.n9nik.videocompressor
**App Name:** TinyVid - Video Compressor
**Version:** 1.0.0 (1)

## What changed from TinyPic base

- Domain logic replaced with `VideoCompressor.kt`: offline video compression via **Media3 Transformer** (`androidx.media3:media3-transformer:1.10.0`), hardware-accelerated — NOT FFmpeg (competitors' 20-minute FFmpeg encodes overheat phones).
- Anti-bloat guards: never upscales (skips Presentation effect when source fits target box), never exceeds source bitrate (`min(preset, source)`), so output is always smaller.
- UI: PickVisualMedia video + GetContent fallback, thumbnail via MediaMetadataRetriever, Small/Medium/High quality chips with estimated output size, live % progress, before/after size + % saved, save to MediaStore Movies/TinyVid (Q+) or legacy + MediaScanner, Share via ACTION_SEND.
- Manifest: `WRITE_EXTERNAL_STORAGE` maxSdkVersion 28 for pre-Q legacy save. PickVisualMedia needs no storage permission.
- Removed exifinterface dep (not needed for video).
- Tests: formatBytes, presets sanity, estimate scaling, formatDuration.
- Strings: app_name updated. Settings: rootProject.name = TinyVid-VideoCompressor.
- Workflow: fails fast when signing secrets missing (refuses unsigned release AAB in CI).

## How to build locally (Android SDK required)

```bash
cd /path/to/TinyVid
./gradlew clean assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Signing

- New upload keystore generated with `keytool` (proper JKS, alias `tinyvid`, CN=TinyVid O=n9nik), verified with `keytool -list` (generic + alias).
- Credentials live ONLY in GitHub Secrets (`UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`). Local copies destroyed after upload.

## Icon - Sep 10 2026

- Generated 3 concepts via media.generate_icon (flat, clean, high contrast, TinyPic family style):
  - A: Play button + inward compression arrows in film frame (selected — direct sibling of TinyPic's photo + arrows icon)
  - B: Shrinking film strip curl with down arrow
  - C: 'V' letterform fused with play triangle in gauge ring
- Best exported 512px, resized via PIL to mipmap densities (48/72/96/144/192) for ic_launcher.png, ic_launcher_round.png, ic_launcher_foreground.png.
- Manifest: `android:icon="@mipmap/ic_launcher"`.

## Ads

- Google sample AdMob IDs for closed testing (app `ca-app-pub-3940256099942544~3347511713`, banner `ca-app-pub-3940256099942544/6300978111`). Real IDs required before Production.

## Offline guarantee

Core compress works airplane mode. Internet permission only for ads.
