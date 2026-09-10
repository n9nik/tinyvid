# TinyVid - Video Compressor

Offline video compressor for Android. Pick a video, choose a quality (Small 360p / Medium 720p / High 1080p), compress entirely on-device with hardware-accelerated encoding (Media3 Transformer) — no upload, no watermark, no account.

- **Package:** `com.n9nik.videocompressor`
- **Design formula:** offline-first, single-purpose, Material 3 minimalist UI, no account, no cloud, no watermark. INTERNET permission only for ads.
- **Ads:** Google Mobile Ads banner + UMP consent. Google sample IDs in closed testing; real IDs required before Production (`PRODUCTION=true` enforces).

## Build

Cloud builds via GitHub Actions (`.github/workflows/android-cloud-build.yml`), same proven setup as TinyPic:

- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`
- Release minification OFF (TinyPic 1.0.0 proved R8 crashes real devices)
- Signed AAB from GitHub Secrets: `UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`
- Keystore generated with `keytool` as proper JKS; decoded with `printf | base64 --decode` (never `echo`)

See the app factory `BUILD_PLAYBOOK.md` for every wall hit building TinyPic — do not repeat them.

## Docs

- `BUILD_NOTES.md` — what changed from the TinyPic base
- `play-listing.md` — Play Store listing draft
- `privacy-policy.md` — privacy policy text
