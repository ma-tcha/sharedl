# ShareDL

Android app that receives a link from the system **Share** menu, downloads the media with `yt-dlp`, keeps a local copy by default, then reopens the Android share sheet to send the file to another app.

Supported today: Instagram (posts, Reels, Stories), with session handling for media that requires one. `yt-dlp` supports many other sites; the app can be extended to them without changing the architecture.

## Features

- Receives a link via the "Share" menu from any app.
- Downloads in the background with progress tracking.
- Optional local copy (toggle on/off), listed on the home screen with a button to re-share each one.
- Instagram login via WebView when the media requires it, with the session reused automatically for later downloads.

## Usage

1. Share the link of a post, Reel, or Story to **ShareDL**.
2. The download starts automatically (the "Save a copy on the phone" preference is on by default).
3. When it's done, the Android share sheet opens with the video file.
4. Saved copies show up on the home screen; a button lets you re-share each one.

If Instagram requires a session, the app opens its login page in a WebView. Sign in, then tap **Use this session**: the download resumes automatically. You can also open **Connect Instagram** before sharing a link. **Disconnect Instagram** deletes the session stored by the app.

## Storage and privacy

- Saved videos live in the app's private external folder: `Android/data/app.sharedl/files/Movies/ShareDL/`.
- Instagram cookies are kept as a Netscape-format file in the app's private storage, excluded from Android backups. They are never logged or shared with other apps.
- If the local copy is disabled, the video is placed temporarily in the cache for sharing, then not kept.

## Download engine

The project uses `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2`, which bundles `yt-dlp` and Python. Extractors depend on the target site's access mechanisms and can break after a site change. Logging in can help when the media requires a session; it does not guarantee success in case of rate limiting or media unavailability.

The dependency ships `arm64-v8a` and `x86_64` ABIs only: 32-bit Android devices are not supported. It noticeably increases the app size.

## Building

Open the folder in Android Studio and run the Gradle task `app > Tasks > build > assembleDebug`. Android SDK 35 is required by the current configuration (`minSdk` 26).

## Installing a published build

Every `vX.Y.Z` tag triggers a CI build that publishes a signed APK on the repo's [Releases](../../releases) page. On GrapheneOS, a tool like Obtainium can track these GitHub Releases and offer automatic install/update of the APK.

## Disclaimer

Personal project for private use. Respect the terms of service of the sites involved and the rights of the creators of the downloaded content.
