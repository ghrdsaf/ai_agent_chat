# Next Build Tasks

## M1: Chrome Extension MVP

1. Create `collectors/browser-extension/`.
2. Add Manifest V3 extension scaffold.
3. Add a content script that detects supported merchant-service pages.
4. Implement the first PDD web adapter.
5. Extract the latest visible customer message from the current conversation.
6. Add copy and fill-draft actions.

## M2: Backend Message and Suggestion APIs

1. Add extension authentication.
2. Add platform-account registration.
3. Add message ingest API.
4. Add reply suggestion API.
5. Persist conversations, messages, suggestions, and operator actions.
6. Add rate limits and request logging.

## M3: AI Service

1. Add intent classification.
2. Add merchant knowledge retrieval.
3. Add reply generation.
4. Add risk classification.
5. Return evidence and reason codes.

## M4: Knowledge Base Console

1. Add FAQ management.
2. Add shipping, refund, product, and tone rules.
3. Add suggestion history.
4. Add rejected/edited suggestion feedback.

## M5: Quality and Commercial Validation

1. Track suggestion generation count.
2. Track copy/fill rate.
3. Track human edit and rejection rate.
4. Track common unsupported intents.
5. Decide the next platform based on real merchant demand.

## Later

1. Add more web adapters: Douyin Shop, Kuaishou Shop, Qianniu.
2. Add PC desktop assistant for native clients.
3. Add Android/ADB visual adapter only after PC route has paying users.
