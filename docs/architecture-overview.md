# Architecture Overview

## Core Principle

先做电脑端 Copilot，后做全自动和移动端。

系统的长期目标是多平台复用，但第一阶段要优先验证商业价值：商家是否愿意每天使用 AI 建议回复、是否愿意付费、知识库是否能降低重复客服成本。

## Target Architecture

```text
Merchant Web Customer Service Page
  -> Chrome Extension Content Script
  -> Extension Sidebar / Popup
  -> Spring Boot Backend
  -> Message Store / Knowledge Base / Risk Policy
  -> Python AI Service
  -> Reply Suggestion
  -> Human Review
  -> Copy / Fill Draft / Optional Low-risk Send
```

## Module Ownership

### Chrome Extension

- Detect supported platform pages.
- Read visible customer messages from the current web customer-service conversation.
- Send normalized message events to the backend.
- Display AI suggestions in a sidebar or popup.
- Support copy, fill draft, and later low-risk send actions.

### Spring Boot Backend

- Own business truth.
- Manage merchants, platform accounts, conversations, messages, knowledge bases, reply suggestions, risk policies, and subscription limits.
- Provide APIs for the Chrome extension and management console.
- Persist evidence needed for debugging and quality review.

### Python AI Service

- Own AI capability.
- Classify customer intent.
- Retrieve merchant knowledge base and FAQ content.
- Generate suggested replies.
- Return risk level, evidence, and suggested next action.

### Frontend Console

- Manage merchant knowledge bases.
- Review conversation history and reply quality.
- Configure reply style, working hours, risk rules, and platform accounts.
- Show usage, conversion, and handoff metrics.

### Future Collectors

Collectors are adapter implementations, not product centers.

- PC browser extension collector: first priority.
- PC desktop assistant collector: later for native clients such as Qianniu PC or WeChat PC.
- Android/ADB visual collector: later for app-only platforms or mobile-first merchants.

## Platform Adapter Contract

Each platform implementation should eventually map to the same logical contract:

```text
detectPage()
extractCurrentConversation()
extractLatestCustomerMessages()
fillReplyDraft(text)
sendReplyIfAllowed(text)
recoverFromError()
```

The rest of the system should not care whether the message came from DOM, Windows UI Automation, OCR, or ADB.

## Why Not Start with ADB

Android/ADB is powerful but slows down the first commercial validation:

- Requires device or emulator setup.
- Needs screenshot/OCR/coordinate stability work.
- Has more compatibility issues around resolution, input method, and app updates.
- Makes customer onboarding harder.

ADB remains a future expansion path after the PC workflow proves demand.

## Why Not Build on a PDD-specific SDK First

PDD-specific Playwright/SDK-style projects can be faster for one platform, but they bind the product to one platform's internal page structure, requests, and behavior. This project should use PDD as the first commercial scenario, not as the system boundary.

The product boundary is:

> AI customer-service assistant for multi-platform e-commerce merchants.
