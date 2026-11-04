# skanr

A light Android document scanner with no ads, no account and no tracking. Scans stay on the phone until you share them.

- Scanning uses Google's [ML Kit document scanner](https://developers.google.com/ml-kit/vision/doc-scanner) (edge detection, cropping, filters). It runs on the device and is delivered through Google Play services.
- Text recognition is bundled in the app, so name suggestions, search and searchable PDFs work offline.
- PDFs are written directly from the scanned JPEGs, with an optional invisible text layer.

## Build

Needs JDK 17+ and the Android SDK (compileSdk 36).

```sh
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release build only includes the 64-bit ARM native libraries to keep the APK small (about 16 MB).

## Design

`design/skanr-mockup.html` is the screen mockup and `design/logo.svg` the logo. `tools/make_logo.py` regenerates the logo from the Geist font: `python3 tools/make_logo.py app/src/main/res/font/geist.ttf design/logo.svg`.

Fonts: [Geist](https://github.com/vercel/geist-font) and [Instrument Serif](https://github.com/Instrument/instrument-serif), both under the SIL Open Font License (see `licenses/`).
