# Manual media checklist — types that can't be reliably automated

The Home chat attachment sheet offers 8 media types. The automated suite
(`MediaPreviewTest`, run via `--home`) fully covers the ones that use the app's
**own** UI. The types below hand off to **Android system UI**, the **camera**,
or an ambiguous picker, which automate flakily across devices/OS versions — so
they are verified as "the option opens its next screen" automatically
(`HomeChatTest.eachMediaTypeOpens`) and sent **manually** using this checklist.

## Automated end-to-end (no manual step needed)
- **Image (Gallery)** — select → preview (caption) → editor (paint/crop/text) → send → bubble, loader, open viewer, reply.
- **Video Gallery** — select → preview → send (skips automatically if no video is on the device; push an `.mp4` to `/sdcard/Movies` to include it).

## Manual — do these by hand each release

For each: open a chat unit you can post to → tap the attachment (📎) → pick the type.

| Type | Steps | Expect |
|---|---|---|
| **Document** | Pick **Document** → system file picker → choose a PDF/doc | File uploads, document bubble shows name + size, tap opens/downloads it |
| **Audio** | Pick **Audio** → system audio picker → choose a track | Audio uploads, audio bubble with play control, playback works |
| **Contact** | Pick **Contact** → system contacts → choose a contact | Contact is posted as a message |
| **Location** | Pick **Location** → location/map picker → confirm | Location is shared to the chat |
| **Photo (camera)** | Pick **Photo** → camera → capture → preview → send | Photo uploads and shows in chat |
| **Video (camera)** | Pick **Video** → camera → record → preview → send | Video uploads and shows in chat |

## Post-send to verify manually on the above
- Upload **loader/progress** runs and completes.
- Received media (from another account) shows a **download** control and downloads.
- Tapping the media opens the **viewer/player**.
- **Long-press → Reply** on the media posts a reply.

(The image path already verifies loader, viewer-open, download-control and
reply automatically; use those as the reference for how each type should behave.)
