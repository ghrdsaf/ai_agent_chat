# Chrome Extension Message Capture Design

## Goal

Use a Chrome extension to capture messages from web-based merchant customer-service workstations, then send normalized events to the backend for AI reply suggestion.

The first implementation should use DOM extraction, not YOLO/OCR. YOLO/OCR is reserved for future desktop or Android visual automation when DOM access is unavailable.

## Scope

### In Scope

- Detect supported web customer-service pages.
- Extract the current platform account hint.
- Detect the active conversation.
- Extract visible customer messages.
- Extract text, image URLs, product cards, and order-card text when available in DOM.
- Deduplicate messages.
- Send normalized message events to Spring Boot.
- Receive reply suggestions.
- Show suggestions in extension UI.
- Copy or fill draft into the page input box.

### Out of Scope for MVP

- Fully unattended bulk auto-send.
- Hidden platform API reverse engineering.
- YOLO/OCR screen recognition.
- Android/ADB control.
- WeChat personal-account automation.

## Extension Architecture

```text
Chrome Extension
  manifest.json
  background service worker
  content scripts
  platform adapters
  sidebar / popup UI
```

### Content Script

Runs inside the merchant workstation page.

Responsibilities:

- Identify whether the current page is supported.
- Load the correct platform adapter.
- Observe DOM changes.
- Extract new messages.
- Fill reply drafts when the merchant clicks an action.

### Background Service Worker

Runs as the extension's backend bridge.

Responsibilities:

- Store extension auth token.
- Call Spring Boot APIs.
- Manage retry and rate limit.
- Route messages between content script and sidebar/popup.

### Sidebar / Popup

Responsibilities:

- Show current conversation status.
- Show AI suggestions.
- Show risk level and reason.
- Provide copy and fill-draft actions.
- Show errors such as unsupported page, login expired, or backend unavailable.

## Platform Adapter Contract

Each platform web adapter should expose the same shape:

```ts
export interface WebPlatformAdapter {
  platform: 'pdd' | 'douyin' | 'kuaishou' | 'qianniu';

  isSupportedPage(context: PageContext): boolean;

  getPlatformAccountHint(context: PageContext): PlatformAccountHint | null;

  getCurrentConversation(context: PageContext): ConversationSnapshot | null;

  extractVisibleMessages(context: PageContext): ExtractedMessage[];

  findReplyInput(context: PageContext): HTMLElement | null;

  fillReplyDraft(context: PageContext, text: string): Promise<void>;
}
```

Recommended shared types:

```ts
export interface PageContext {
  url: string;
  title: string;
  document: Document;
}

export interface PlatformAccountHint {
  shopName?: string;
  accountName?: string;
  rawText?: string;
}

export interface ConversationSnapshot {
  externalConversationKey: string;
  buyerName?: string;
  title?: string;
  rawText?: string;
}

export interface ExtractedMessage {
  externalMessageKey: string;
  direction: 'customer' | 'merchant' | 'system';
  messageType: 'text' | 'image' | 'product_card' | 'order_card' | 'mixed';
  text?: string;
  imageUrls?: string[];
  linkUrls?: string[];
  occurredAt?: string;
  rawHtmlHash?: string;
  domPathHint?: string;
}
```

## Message Capture Strategy

### 1. Page Detection

The content script checks:

- Current URL host/path.
- Known page title patterns.
- Presence of stable root elements.
- Presence of chat list, message list, or reply input.

Example:

```text
if URL matches PDD merchant workstation
and chat root exists
and reply input exists
then load pdd-web-adapter
```

Do not depend on one selector only. Use a small set of signals so minor page changes do not break detection.

### 2. Active Conversation Detection

The adapter should create a stable `externalConversationKey`.

Preferred sources:

1. Conversation ID from DOM attributes if visible.
2. Buyer/user ID from visible attributes if available.
3. URL query parameter if the workstation exposes it.
4. Fallback hash from buyer name + current page context + first visible message.

The fallback is acceptable for MVP but should be marked as lower confidence.

### 3. Message Extraction

The adapter scans the visible message list and extracts message nodes.

For each node:

- Determine direction: customer, merchant, or system.
- Extract text with `innerText`.
- Extract images from `img.src`, `data-src`, `srcset`, or preview links.
- Extract product/order card text from nested text.
- Extract links from `a.href`.
- Build `externalMessageKey`.

Preferred message key:

```text
platform + externalConversationKey + platformMessageId
```

Fallback message key:

```text
platform + externalConversationKey + sha256(direction + messageType + text + imageUrls + rawHtmlHash)
```

### 4. DOM Change Observation

Use `MutationObserver` on the message-list root.

Behavior:

- Debounce changes for 300-800 ms.
- Re-scan visible messages after each change.
- Send only newly discovered customer messages.
- Re-scan when the active conversation changes.

Avoid high-frequency polling as the default. A light fallback poll every 3-5 seconds is acceptable for recovery.

### 5. Deduplication

Deduplicate in two layers.

In extension memory:

- Keep a recent LRU set of `externalMessageKey`.
- Reset when the active conversation changes.

In backend:

- Enforce uniqueness by platform account + external conversation key + external message key.
- Ignore duplicate ingest requests idempotently.

### 6. Image Message Handling

For PC web workstations, image messages do not need YOLO.

Preferred handling:

- Extract image URL or blob URL from DOM.
- If URL is accessible by backend, send URL.
- If only accessible in browser session, extension fetches the image as a blob and uploads it to backend.
- Backend forwards the image or stored object URL to the multimodal AI service.

Do not train YOLO just to understand buyer-uploaded images. Use a multimodal model first.

### 7. Reply Draft Fill

The content script should fill the platform's reply input only after user action.

Implementation order:

1. Find editable input:
   - `textarea`
   - `input`
   - `[contenteditable="true"]`
2. Focus it.
3. Set value or text content.
4. Dispatch input/change/composition events so frontend frameworks detect the change.
5. Do not click send in MVP.

Auto-send can be added later only for low-risk, allowlisted intents.

## Backend API Shape

### Register or Resolve Platform Account

```http
POST /api/extension/platform-accounts/resolve
```

Request:

```json
{
  "platform": "pdd",
  "shopName": "Demo Shop",
  "accountName": "Agent A",
  "pageUrl": "https://..."
}
```

Response:

```json
{
  "platformAccountId": "pa_123",
  "status": "active"
}
```

### Ingest Messages

```http
POST /api/extension/messages/ingest
```

Request:

```json
{
  "platformAccountId": "pa_123",
  "platform": "pdd",
  "conversation": {
    "externalConversationKey": "conv_xxx",
    "buyerName": "Buyer Nickname",
    "title": "Current Conversation"
  },
  "messages": [
    {
      "externalMessageKey": "msg_xxx",
      "direction": "customer",
      "messageType": "text",
      "text": "When will this ship?",
      "occurredAt": null,
      "rawHtmlHash": "..."
    }
  ]
}
```

Response:

```json
{
  "accepted": 1,
  "duplicates": 0,
  "suggestions": [
    {
      "messageKey": "msg_xxx",
      "suggestionId": "rs_123",
      "text": "This item normally ships within 24 hours. Tracking will update after dispatch.",
      "riskLevel": "low",
      "action": "human_confirm"
    }
  ]
}
```

### Record Operator Action

```http
POST /api/extension/operator-actions
```

Request:

```json
{
  "suggestionId": "rs_123",
  "action": "fill_draft",
  "finalText": "This item normally ships within 24 hours."
}
```

## Data Model Additions

Minimum backend fields needed for the extension route:

```text
platform_account
- id
- merchant_id
- platform
- shop_name
- account_name
- status
- created_at

conversation
- id
- platform_account_id
- external_conversation_key
- buyer_name
- title
- last_message_at

message
- id
- conversation_id
- external_message_key
- direction
- message_type
- text
- image_asset_ids
- raw_html_hash
- occurred_at
- created_at

reply_suggestion
- id
- message_id
- suggestion_text
- intent
- risk_level
- action
- evidence_json
- created_at

operator_action
- id
- suggestion_id
- action
- final_text
- created_at
```

## Reliability Rules

- Never assume the platform DOM is stable.
- Keep selectors inside platform adapters.
- Prefer multiple weak signals over one fragile selector.
- Keep DOM extraction read-only unless the user clicks copy/fill.
- Log extraction failures with platform, URL pattern, and adapter version.
- Store enough evidence to debug incorrect replies without storing unnecessary sensitive data.

## Security and Compliance Rules

- Only capture pages explicitly supported by the extension.
- Only process merchant-authorized accounts.
- Do not collect unrelated browsing data.
- Do not capture hidden platform data that is not visible to the merchant.
- Do not auto-send by default.
- Provide a clear pause switch in the extension UI.

## MVP Implementation Order

1. Scaffold `collectors/browser-extension`.
2. Add Manifest V3 extension.
3. Add background service worker and content script.
4. Add adapter interface and `pdd-web-adapter`.
5. Detect supported page and active conversation.
6. Extract visible text messages.
7. Add backend ingest API.
8. Add AI suggestion call.
9. Display suggestions in sidebar or popup.
10. Add copy and fill-draft actions.
11. Add image URL extraction.
12. Add image upload path for session-only images.
