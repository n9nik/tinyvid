# TinyVid - Video Compressor — Play Store Listing Draft

**Package:** `com.n9nik.videocompressor` (debug variant `com.n9nik.videocompressor.debug` for testing)
**App Name:** TinyVid - Video Compressor
**Category:** Video Players & Editors / Tools
**Content Rating:** Everyone
**Pricing:** Free, ad-supported, no in-app purchases, no paywall

## Short description (80 chars max)

Offline video compressor. Shrink videos fast — no watermark, no signup.

## Full description

**TinyVid shrinks your videos offline — no watermark, no cloud upload, no account.**

Other video compressors make you watch five ads per compression, take 20 minutes to encode, overheat your phone, and sometimes even make the file BIGGER. TinyVid does one job, fast, on your phone.

**Why people switch to TinyVid:**
- ✅ **True offline** — works in airplane mode, never uploads your videos
- ⚡ **Hardware encoding** — minutes, not 20-minute overheating encodes
- 🎯 **Small / Medium / High** — 360p, 720p, 1080p presets with estimated size before you start
- 📉 **Never inflates** — never upscales, never exceeds your original bitrate
- 🚫 **No watermark** — ever. Your video stays yours
- 🔒 **Privacy first** — only reads the video you pick, no contacts/location creep
- 📱 **Small app** — no bloat, fast by default

**Perfect for:**
- WhatsApp / Telegram videos that won't send
- Email attachments with size limits
- Freeing up phone storage without deleting memories
- Uploading to forms, portals, and marketplaces

**How it works:**
1. Pick video from system picker
2. Choose Small / Medium / High quality
3. Tap Compress — 100% offline, hardware encoded
4. See before/after size + % saved, save to Movies/TinyVid or share

No account. No cloud. No watermark. Just compression.

**Vs. competitors (1-star patterns we fixed):**
- Other app: "had to watch like 5 ads" per compression → TinyVid: banner only, never per-compress ad spam
- Other app: "compressed 100MB to 469MB???" → TinyVid: never upscales, never exceeds source bitrate
- Other app: "21-minute encode overheated my phone" → TinyVid: hardware encoding, minutes
- Other app: "uploads to cloud privacy fear" → TinyVid: offline, verify with airplane mode

**Tech:** Media3 Transformer with hardware encoder, resolution scaling via Presentation effect, capped bitrate.

**Free, ad-supported** to keep it alive — ads never block your compress.

---

## Keywords (for ASO, not in description directly)

video compressor, compress video, reduce video size, shrink video, video size reducer, no watermark video compressor, offline video compressor, compress video for WhatsApp, MP4 compressor

## Store assets TODO

- Icon: 512x512 (done — play button + compression arrows, TinyPic family style)
- Screenshots (truthful):
  1. Picker + original video card (size, resolution, duration)
  2. Quality chips Small/Medium/High with estimate
  3. Progress + compressed result + % saved
  4. Save to Movies/TinyVid + Share
  5. Airplane mode badge "Works offline"
- Feature graphic: "Offline video compression • No Watermark" text, high contrast
- Privacy policy URL: host `privacy-policy.md`
- Data Safety:
  - Does app collect/share data? Yes, via Google ads SDK: Advertising ID, approximate location (ad), app interactions. No collection by developer directly. No upload of videos.
  - Encryption in transit: Yes (ads SDK)
  - Data deletion: No account to delete, cache cleared on uninstall, user can delete saved videos via gallery
  - Location: Approximate only via ads SDK, not precise, user can opt out via device ad settings

## Monetization

- Banner only in v1 (sample IDs in closed testing, real IDs at Production)
- No paywall, no subscription

## Release checklist

- Replace sample AdMob IDs with real IDs before Production
- Generate AAB via `./gradlew bundleRelease`
- Complete Play App Signing, content rating, target audience, ads declaration yes, Data Safety yes
- Upload AAB to closed testing track, 12 testers × 14 days (per-app rule)

---
*Drafted Sep 10 2026 PDT for Nikhil's factory. Keep under 4000 chars for full description Play limit.*
