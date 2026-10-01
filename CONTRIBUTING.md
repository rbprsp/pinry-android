# Contributing

Thanks for helping. Bug reports and pull requests go to [GitHub](https://github.com/rbprsp/pinry-android). Development happens in the primary repository at [git.relony.dev](https://git.relony.dev/relony/pinry-android), which is mirrored to GitHub, so an accepted pull request is merged there and shows up on GitHub with the next sync (the PR may appear as closed rather than merged).

## Reporting bugs

Use the bug report template. The Pinry server version, whether the server is public or private, and how it's reached (https, http on a LAN, a reverse proxy) matter more often than you'd think.

Security problems: see [SECURITY.md](SECURITY.md), not the issue tracker.

## Building

- JDK 21 and the Android SDK (compile SDK 37).
- `./gradlew assembleDebug` builds, `./gradlew testDebugUnitTest` runs the unit tests, `./gradlew :app:lintRelease` must stay clean.
- Release signing: copy `keystore.properties.example` to `keystore.properties`; without it, release builds are unsigned.

## Code

- Kotlin with the official code style; match the code around you.
- Material 3 Expressive APIs are still alpha. Keep `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` inside `ui/theme` and `ui/common`, so a library update only touches those.
- Behavior of the Pinry API that looks odd usually is (tags wiped by a PATCH without them, `pins__id` meaning the board id, private instances needing the session cookie). Check `data/` and its tests before "fixing" it.
- Add or update tests for changes in `data/` and the pager.

## Baseline Profile

After changes to startup, the feed or pin detail, regenerate the profile on a device or emulator (the command is in `BaselineProfileGenerator.kt`) and commit the updated files under `app/src/release/generated/baselineProfiles/`.

By contributing you agree that your contribution is licensed under the GPL-3.0-or-later, like the rest of the project.
