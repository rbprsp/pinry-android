# Security policy

## Reporting a vulnerability

Please email **relonychan@proton.me** with `pinry-android security` in the subject, and don't open a public issue for it. Include the app version, what an attacker can do, and steps to reproduce. You'll get an answer as soon as possible; fixes ship in a new release.

Only the latest release is supported.

Problems in the Pinry server itself belong to the [Pinry project](https://github.com/pinry/pinry).

## How the app handles your account

- The app uses your password once to log in and never stores it.
- It keeps the API token and Django session cookie your server hands out in its private storage, excluded from cloud backup and from device-to-device transfer.
- It sends them only to the server you logged in to (same scheme, host and port). Images and link previews from other hosts never get them.
- Logging out ends the session on the server. Pinry's API tokens don't expire and the API can't revoke them; to invalidate one, delete it in the server's Django admin (*Auth Token → Tokens*).
- There are no analytics, crash reporting or ads. See [PRIVACY.md](PRIVACY.md).

## Deliberate trade-offs

This is a client for self-hosted servers, so two defaults are looser than for a typical app:

- Plain `http://` is allowed for servers on a home network. The login screen warns when it's used: the password and everything else travel unencrypted.
- User-installed certificate authorities are trusted, so servers with a private or self-signed certificate work. Anyone able to install a CA on your device could read the app's traffic.

## Input from others

Pins, descriptions and links are written by every user of a Pinry server, and other apps can share content into this one. The app therefore only opens `http(s)` links, accepts shared files only as `content://` URIs, and resolves link previews without your session.
