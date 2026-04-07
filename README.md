# ZBrowser

A lightweight advanced web browser for Android, built entirely in Termux.

## Features

- **Tab Browsing** - Multiple tabs with easy management
- **Incognito Mode** - Private browsing that doesn't save history
- **Download Manager** - Built-in file downloads
- **Desktop Mode** - Toggle desktop user agent for full website viewing
- **Night Mode** - Dark theme for comfortable reading
- **Bookmarks** - Save and manage your favorite sites
- **Find in Page** - Search text within webpages
- **Share & Copy URL** - Easy sharing and clipboard support
- **Fullscreen Video** - Watch videos in fullscreen mode
- **External App Dialog** - Security confirmation before opening other apps

## Prerequisites

### Termux Setup

ZBrowser is designed to be built in Termux on Android. To set up the build environment:

1. **Install Termux** from F-Droid or GitHub releases

2. **Configure [termuxvoid repository](https://termuxvoid.github.io/)** (required for android-sdk):

```bash
# Install Android SDK
apt install android-sdk
```

### Build with Gradle

```bash
# Navigate to project directory
cd ZBrowser

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Or using bash
bash gradlew assembleDebug
```

### Output

The built APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Clean Build

```bash
./gradlew clean
./gradlew assembleDebug
```

## Project Structure

```
ZBrowser/
├── app/
│   ├── src/main/
│   │   ├── java/alienkrishn/zbrowser/
│   │   │   ├── MainActivity.java      # Main browser activity
│   │   │   ├── SettingsActivity.java  # Settings screen
│   │   │   ├── adapter/
│   │   │   │   └── TabsAdapter.java   # Tabs list adapter
│   │   │   └── model/
│   │   │       └── TabInfo.java       # Tab data model
│   │   ├── res/
│   │   │   ├── layout/                # UI layouts
│   │   │   ├── menu/                  # Menu definitions
│   │   │   ├── drawable/              # Icons and graphics
│   │   │   └── values/                # Strings, colors, styles
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── gradle.properties
└── settings.gradle
```

## Configuration

### SDK Versions (as configured)

| Setting | Version |
|---------|---------|
| compileSdk | 36 |
| minSdk | 29 |
| targetSdk | 34 |
| buildToolsVersion | 36.1.0 |
| ndkVersion | 29.0.14206865 |
| Java Version | 17 |

## Installation

1. Build the APK as shown above
2. Transfer `app-debug.apk` to your device (if building remotely)
3. Install the APK on your Android device

## Acknowledgments

This project is based on [MkBrowser](https://github.com/mengkunsoft/MkBrowser) by Mengkun, which provided the foundation for this lightweight browser implementation.

## License

MIT License - See original MkBrowser license for reference.

## Notes

- This app is built using Termux with android-sdk installed via apt
- The termuxvoid repository must be configured to access the android-sdk package
- All builds are debug builds suitable for development and testing
- For production builds, configure signing in `app/build.gradle`
