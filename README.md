# YouTorrent

Android 17 torrent downloader built around Jetpack Compose and Material 3 Expressive.

## direction

YouTorrent is intended to combine a clean, adaptive Android 17 interface with a full BitTorrent engine. The product direction is inspired by the feature set of LibreTorrent while keeping YouTorrent's UI, architecture, and implementation independent.

### stack

- Android 17 / API 37
- Kotlin 2.4.21
- Android Gradle Plugin 9.4.1
- Jetpack Compose + Material 3
- dynamic color and adaptive layouts
- libtorrent4j 2.1.0-38 for the torrent engine

### current foundation

- Compose-first Android 17 project
- Material 3 themed shell with dynamic system colors
- torrent list with live libtorrent4j progress, speed and peer state
- magnet-link entry flow and Android magnet deep links
- pause/resume/remove actions backed by the real torrent session
- persistent torrent metadata with restore-on-launch
- foreground torrent service and notification
- GitHub Actions debug APK build on pushes and pull requests
- optional release APK signing through environment variables

### release signing

The release build supports signing without committing a keystore to the repository. Set these environment variables before running `assembleRelease`:

- `SIGNING_STORE_FILE` — path to the `.jks`/`.keystore` file
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

When all four values are present, the release variant uses the supplied signing key. Otherwise the release variant remains unsigned so CI and local builds do not require secrets.

For GitHub Actions, keep the keystore and passwords in repository/environment secrets and materialize the keystore only during the release workflow. Never commit the keystore or passwords to Git.

### roadmap

1. resume-data persistence for true session recovery
2. torrent detail screen with files, priorities and trackers
3. .torrent file picker and richer notification actions
4. file selection, priorities and sequential download
5. network/proxy, limits, scheduling and storage settings
6. adaptive phone/tablet/desktop layouts for Android 17
7. accessibility, motion and Material 3 Expressive polish

## references

- Material Design resources: https://m2.material.io/resources
- LibreTorrent: https://github.com/proninyaroslav/libretorrent
- Material Design in XAML Toolkit: https://github.com/materialdesigninxaml/materialdesigninxamltoolkit

## license

GPL-3.0. See `LICENSE`.
