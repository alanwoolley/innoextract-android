# Inno Setup Extractor for Android

[![Build Status](https://github.com/alanwoolley/innoextract-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/alanwoolley/innoextract-android/actions/workflows/android-ci.yml)

A port of the excellent [Innoextract](https://constexpr.org/innoextract/) tool to Android, allowing you to extract the contents of [Inno Setup](https://jrsoftware.org/isinfo.php) installer files (`.exe`) directly on your Android device — no PC required.

This project wraps the original `innoextract` C++ codebase with an Android user interface, enabling quick and portable extraction of installer contents.

## Features

- Extracts data from **Inno Setup** installers (`.exe` files)
- Supports most Inno Setup versions and compression methods
- Works entirely on your Android device — no internet connection required
- Based on the original, proven [`innoextract`](https://constexpr.org/innoextract/) open-source project

## Download

<a href="https://play.google.com/store/apps/details?id=uk.co.armedpineapple.innoextract&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1">
  <img src="https://play.google.com/intl/en_gb/badges/images/generic/en_badge_web_generic.png" width="200" alt="Get it on Google Play">
</a>

## Usage

1. Install the app from [Google Play](https://play.google.com/store/apps/details?id=uk.co.armedpineapple.innoextract).
2. Open the app and select an Inno Setup installer file (`.exe`) from your device storage.
3. Choose an output folder.
4. Tap **Extract** — the files will be unpacked into the chosen folder.

**Note:** This tool cannot run or install Windows programs — it only extracts the contents.

## Building from Source

To build the app yourself:

### Requirements

- Android Studio (latest stable)
- Android NDK (for native C++ build)
- Git

### Steps

```bash
# Clone the repository
git clone https://github.com/alanwoolley/innoextract-android.git
cd innoextract-android

# Open the project in Android Studio
# The IDE will handle Gradle sync and build
```

## Upstream Project

This app is based on the cross-platform command-line tool [`innoextract`](https://constexpr.org/innoextract/) by Daniel Scharrer.  
Upstream source: [https://github.com/dscharrer/innoextract](https://github.com/dscharrer/innoextract)

## Contributing

Contributions, bug reports, and feature requests are welcome!  
Please:
1. Open an issue to discuss your idea or bug.
2. Submit a pull request with clear commit messages.


## License

This project is available under the [MIT License](LICENSE).


## More Information

You can read more about this project on my website:  
[https://www.armedpineapple.co.uk/projects/inno-setup-extractor/](https://www.armedpineapple.co.uk/projects/inno-setup-extractor/)