# Privacy

Pinry for Android has no analytics, no ads, no crash reporting and no server of its own. It talks to:

| Where | What | Why |
|---|---|---|
| Your Pinry server | Your login, the pins, boards and tags you browse, images you upload | That's the app |
| gravatar.com | An MD5 hash of each pin author's email address (Pinry provides it) | Avatars next to pin authors |
| Pages you pin from | A request for the page and its preview image, from your phone | Showing a preview when you pin a link; your server downloads the image itself afterwards |

On your phone, the app keeps the session for your server, your settings, and a cache of images you've viewed. Log out, clear the app's storage, or uninstall to remove them. The session is not included in backups or transfers to a new device.
