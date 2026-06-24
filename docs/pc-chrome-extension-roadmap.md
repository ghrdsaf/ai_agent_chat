# PC Chrome Extension Roadmap

## Goal

Build the first commercial MVP as a PC browser-based AI customer-service Copilot.

The first version should help merchants reply faster without requiring Android devices, emulators, ADB, OCR model training, or risky fully unattended automation.

## Product Scope

### MVP

- Detect supported merchant-service pages.
- Read the visible latest customer message in the current conversation.
- Send normalized message content to the backend.
- Generate a suggested reply using merchant knowledge.
- Show the suggestion in a Chrome extension sidebar or popup.
- Support one-click copy and one-click fill draft.
- Store message, suggestion, risk level, and user action.

### Not in MVP

- Fully unattended bulk auto-reply.
- WeChat personal-account automation.
- Android/ADB device control.
- YOLO/OCR visual recognition.
- Reverse engineering hidden platform APIs.
- Multi-platform simultaneous automation.

## Technical Stack

### Chrome Extension

- Manifest V3.
- Content script for DOM extraction.
- Sidebar or popup for suggestion display.
- Background service worker for auth token, API calls, and platform routing.
- Platform adapter modules, for example:
  - `pdd-web-adapter`
  - `douyin-web-adapter`
  - `kuaishou-web-adapter`
  - `qianniu-web-adapter`

### Backend

- Spring Boot.
- PostgreSQL or MySQL as the primary database.
- Redis later for rate limit, queues, and short-lived session state.
- REST APIs for extension and management console.

### AI Service

- Python FastAPI.
- Intent classification.
- Knowledge retrieval.
- Reply generation.
- Risk classification.
- Evidence return for debugging and review.

### Knowledge Base

The knowledge base should support:

- Merchant FAQ.
- Product rules.
- Shipping rules.
- Return and refund rules.
- Tone and reply style.
- Prohibited commitments.
- Escalation rules.

## Data Flow

```text
Customer message visible in browser
  -> Content script extracts message
  -> Extension normalizes platform event
  -> Spring Boot stores message
  -> Spring Boot requests AI suggestion
  -> Python AI service retrieves knowledge and generates response
  -> Spring Boot stores suggestion and risk result
  -> Extension displays suggestion
  -> Merchant copies or fills draft
  -> Backend records action
```

## Platform Adapter Shape

Each browser platform adapter should implement:

```text
isSupportedPage(url, document)
getPlatformAccountHint(document)
getCurrentConversationKey(document)
extractLatestCustomerMessages(document)
findReplyInput(document)
fillReplyDraft(document, text)
```

Avoid leaking platform-specific DOM selectors into business logic. DOM selectors should stay inside the adapter.

## Backend Domain Model

Minimum entities:

- `merchant`
- `platform_account`
- `conversation`
- `message`
- `reply_suggestion`
- `knowledge_document`
- `risk_decision`
- `operator_action`

Important fields:

- Platform name.
- External conversation key.
- Message direction.
- Raw extracted text.
- Normalized text.
- Confidence or extraction source.
- Suggestion text.
- Risk level.
- Whether human approved.
- Whether draft was filled or copied.

## Risk Policy

Default behavior:

- AI can suggest.
- Human confirms.
- Auto-send is disabled.

High-risk intents should always require human handling:

- Refund disputes.
- Compensation promises.
- Bad review threats.
- Abuse or harassment.
- Platform rule disputes.
- Legal or invoice questions.
- Any case requiring order-specific verification if the order context is missing.

## Commercial Validation Metrics

Track these from the first MVP:

- Daily active merchants.
- Suggestions generated per merchant per day.
- Copy/fill rate.
- Human edit rate.
- Rejected suggestion rate.
- Average response time improvement.
- Top repeated intents.
- Knowledge gaps found from unsupported questions.

These metrics decide whether to expand to more platforms, desktop automation, or Android/ADB.

## Expansion Path

### Stage 1: PDD Web Copilot

Implement one platform well. Keep manual confirmation as default.

### Stage 2: Knowledge and Quality

Add better FAQ retrieval, merchant tone configuration, high-risk detection, and suggestion review.

### Stage 3: More Web Platforms

Add Douyin Shop, Kuaishou Shop, and Qianniu web adapters if customer demand exists.

### Stage 4: PC Desktop Assistant

Use Windows UI Automation or screenshot/OCR only for native desktop clients that cannot be handled by a browser extension.

### Stage 5: Android/ADB Visual Adapter

Add ADB screenshot, OCR, YOLO, and simulated input for app-only or mobile-first platforms after the PC route has paying users.
