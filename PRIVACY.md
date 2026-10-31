# Flashback privacy policy

Last updated: 1 November 2026

Flashback is a free, open-source (GPL v3) flashcard app. It has no accounts, no ads, no trackers and no analytics. The developer does not run a server for it and cannot see your decks, your study history or anything else you do in the app.

## What stays on your device

Your decks, cards, tags, study history, statistics and settings are stored in a database on your device. They are never sent to the developer.

You can export your decks to a JSON file and import them again. Where that file goes is up to you.

## The optional AI card generator

The generator is off until you add your own API key in Settings. If you never add one, the app does not connect to the internet.

When you use it:

- The text or PDF you choose, and the options you pick (card count, card types), are sent over HTTPS to the provider you selected: Anthropic (`api.anthropic.com`) or Google (`generativelanguage.googleapis.com`).
- Your API key is sent to that same provider so it can bill your account. The key is paid for by you, under your agreement with the provider.
- Nothing else is sent. The app does not send your existing decks, your study history, a device identifier or any other information.
- The provider handles what you send under its own terms and privacy policy, which may differ between free and paid accounts. Read them before you send anything private, such as other people's personal details.
- Generated cards can contain mistakes. You review them before they are saved.

Your API key is encrypted with a key held in the Android Keystore and stored only on your device. It is left out of Android backups and device transfers. It is not written to logs.

## The prompt builder

"Use an AI website" only builds a block of text for you to copy or share. Flashback sends nothing. What you paste into ChatGPT, Claude, Gemini or any other site, and what that site does with it, is between you and that site.

## Permissions

- **Notifications:** for the optional daily study reminder. You can turn it off in Settings.
- **Internet:** only for the optional generator described above.

## Backups

Flashback allows Android's backup. If backup is on for your device, Android (Google) may store a copy of your decks and settings in your Google account and restore it on a new device. The app does not control this. Your API key is excluded. You can turn it off in your device's system settings.

## Children

Flashback is an offline study tool that children can use. It collects no personal information. A child should only use the AI generator with a parent or guardian, because it needs a paid API key and sends what they enter to a third party.

## Changes

If this policy changes, the new version will be published in the app's source repository with a new date.

## Contact

Open an issue at https://github.com/mowtiie/Flashback/issues.
