# ADR-001: Start with PC Chrome Extension Copilot

## Status

Accepted

## Date

2026-06-24

## Context

The product goal is to build a multi-platform AI customer-service assistant for e-commerce merchants. The long-term architecture should support Pinduoduo, Douyin Shop, Kuaishou Shop, Qianniu, Xianyu, and other merchant-service surfaces.

Several collection strategies are possible:

- Platform-specific SDK or Playwright automation.
- Android/ADB visual automation with OCR and YOLO.
- PC desktop automation.
- Chrome extension for web-based merchant workstations.

The team wants to own the core architecture rather than depend on a PDD-specific SDK implementation. Reuse, maintainability, and future platform expansion matter more than the fastest single-platform shortcut.

## Decision

Start with a **PC Chrome extension + Spring Boot backend + Python AI service + merchant knowledge base**.

The first product shape is a Copilot:

- Read customer messages from supported web customer-service workstations.
- Generate AI reply suggestions.
- Show suggestions to the merchant.
- Support copy or fill-draft actions.
- Keep human confirmation as the default sending path.

Do not start with Android/ADB or a PDD-specific SDK as the primary implementation path.

## Alternatives Considered

### PDD-specific SDK / Playwright Workstation Automation

Pros:

- Fastest route to a Pinduoduo-only MVP.
- Can often access structured page data more accurately than OCR.
- May support message send, order lookup, and workstation behaviors earlier.

Cons:

- Strongly coupled to Pinduoduo's web workstation internals.
- Harder to reuse across Douyin, Kuaishou, Qianniu, and other platforms.
- Increases the chance that the product becomes a PDD script instead of a multi-platform system.
- May encourage relying on non-public behavior that changes without notice.

Rejected as the primary architecture. It can still be studied for feature ideas and business capability mapping.

### Android/ADB Visual Automation First

Pros:

- Platform independent when the target app runs on Android.
- Aligns with the long-term visual automation architecture.
- Can support app-only platforms.

Cons:

- Slower commercial validation.
- Requires emulator or physical-device onboarding.
- Needs OCR, screenshot, resolution, coordinate, input-method, and recovery work before the product feels stable.
- Harder for merchants to install and understand in the first version.

Deferred until after PC Copilot validation.

### PC Desktop Automation First

Pros:

- Can support native desktop clients.
- Useful for Qianniu PC, WeChat PC, and similar tools.

Cons:

- Windows compatibility, DPI scaling, and UI Automation reliability add complexity.
- Less straightforward than a Chrome extension for web workstations.

Deferred until after the browser-extension path proves demand.

## Consequences

- The first collector will be a Chrome extension adapter, not an ADB adapter.
- The backend schema should remain platform-neutral: platform accounts, conversations, messages, suggestions, knowledge-base references, risk decisions, and evidence.
- Sending should be conservative at first: copy or fill draft by default, low-risk automatic sending only after quality and compliance controls exist.
- Future adapters must implement a common logical contract so DOM, desktop, OCR, and ADB collectors can feed the same backend.
- Documentation and task planning should describe this as an AI customer-service Copilot, not as an automatic-reply bot or platform message scraper.
