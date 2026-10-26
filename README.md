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
- empty state and torrent cards
- magnet-link entry flow
- pause/resume UI state
- Android 17 local-network and foreground-service permissions prepared
- libtorrent4j dependencies wired into the app module

### roadmap

1. torrent engine/session layer backed by libtorrent4j
2. persistent torrent database and restore-on-launch
3. .torrent file picker and magnet intents
4. foreground torrent service + notifications
5. real progress, peers, trackers, speed and ETA
6. file selection, priorities and sequential download
7. network/proxy, limits, scheduling and storage settings
8. adaptive phone/tablet/desktop layouts for Android 17
9. accessibility, motion and Material 3 Expressive polish

## references

- Material Design resources: https://m2.material.io/resources
- LibreTorrent: https://github.com/proninyaroslav/libretorrent
- Material Design in XAML Toolkit: https://github.com/materialdesigninxaml/materialdesigninxamltoolkit

## license

GPL-3.0. See `LICENSE`.
