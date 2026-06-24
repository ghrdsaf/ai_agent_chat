# Collectors

Collectors are platform adapters that bring customer-service messages into the unified backend.

The first collector should be a Chrome extension, not an Android/ADB collector.

## Priority

1. `browser-extension/`: Chrome extension for web merchant-service workstations.
2. `desktop-assistant/`: future Windows desktop assistant for native PC clients.
3. `android-adb/`: future Android/ADB visual collector for app-only platforms.

## Collector Responsibilities

- Detect supported platform and account context.
- Extract visible customer messages.
- Normalize platform-specific data into the backend message model.
- Display reply suggestions or send them back to the UI layer.
- Execute conservative actions such as copy or fill draft.
- Report operator actions and errors.

## Non-goals for the First Version

- Fully unattended auto-reply.
- Hidden API reverse engineering.
- Android emulator/device onboarding.
- WeChat personal-account automation.
