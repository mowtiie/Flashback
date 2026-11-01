# Flashback privacy policy

Last updated: 1 November 2026

Flashback is a free, open-source (GPL v3) flashcard app. It has no accounts, no ads, no trackers and no analytics. The developer does not run a server for it and cannot see your decks, your study history or anything else you do in the app.

## What stays on your device

Your decks, cards, tags, study history, statistics and settings are stored in a database on your device. They are never sent to the developer.

You can export your decks to a JSON file and import them again. Where that file goes is up to you.

## The optional AI card generator

The generator is off until you add your own API key or choose a model file in Settings. If you never add a key, the app does not connect to the internet.

### On-device model

Instead of a provider you can choose "On-device model" and pick a model file (for example a Gemma model in the .litertlm format) from your phone. The app copies it into its private storage, and the cards are made on the phone. Nothing you add is sent anywhere, and the app downloads nothing. The model file is left out of Android backups and device transfers. Removing the model in Settings deletes the copy.

### Online providers

When you use it:

- The text or PDF you choose, and the options you pick (coverage, card types), are sent over HTTPS to the provider you selected: Anthropic (`api.anthropic.com`) or Google (`generativelanguage.googleapis.com`). Long text is split into parts and sent as several requests, one after another. Each later request also carries the questions already made, so they are not repeated. The app asks you to confirm before it sends more than three.
- Your API key is sent to that same provider so it can bill your account. The key is paid for by you, under your agreement with the provider.
- Like any internet request, it also reaches the provider with your IP address and standard request headers, which can include your Android version and device model. The app adds no identifier of its own and sends no account, location, contacts, your existing decks or your study history.
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
