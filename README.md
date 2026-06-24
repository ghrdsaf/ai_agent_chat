# ai_agent_chat

面向中小电商商家的多平台 AI 客服助手。

当前产品路线优先做 **电脑端 Chrome 插件 + 后端 Agent + 商家知识库**。第一阶段不做手机 ADB 自动化，也不直接复用平台内部接口 SDK；先用更容易交付、调试和商业验证的 PC 工作台 Copilot 路线，把消息识别、AI 建议回复、知识库、人工确认流程跑通。

## Current Direction

- Chrome 插件作为第一采集端：读取商家 Web 客服工作台中的当前会话消息，展示 AI 建议回复，并支持一键复制或填入输入框。
- Spring Boot 作为业务中台：负责商家账号、平台账号、会话、消息、知识库、回复建议、风险策略和套餐能力。
- Python FastAPI 作为 AI 服务：负责意图识别、知识库检索、回复生成、风险判断和证据返回。
- 先做 Copilot，不默认无人值守自动发送：低风险场景后续再开放自动回复白名单。
- 后续再扩展 PC 桌面助手和 Android/ADB 视觉适配器。

## Project Structure

```text
backend/                Spring Boot business backend
ai-service/             Python FastAPI AI service
frontend/               Management console placeholder
collectors/             Channel collectors and browser-extension adapters
deploy/                 Deployment notes and future Docker/K8s config
docs/                   Architecture docs, ADRs, and build plan
```

## Build Order

1. Chrome 插件 MVP：在拼多多商家 Web 客服工作台中识别当前会话买家消息。
2. 消息入库：统一保存平台账号、会话、消息、截图/证据和处理状态。
3. AI 建议回复：基于商家知识库、FAQ、商品规则生成建议回复。
4. 人工确认工作流：先支持复制/填入草稿，再逐步开放低风险自动发送。
5. 多平台抽象：把拼多多实现沉淀为 `PlatformAdapter`，后续扩展抖店、快手、千牛。
6. 移动端补充：在 PC 路线验证付费后，再接入 Android/ADB + OCR/YOLO 视觉方案。

## Key Documents

- [Architecture Overview](docs/architecture-overview.md)
- [PC Chrome Extension Roadmap](docs/pc-chrome-extension-roadmap.md)
- [ADR-001: Start with PC Chrome Extension Copilot](docs/decisions/ADR-001-start-with-pc-chrome-extension-copilot.md)
- [Next Build Tasks](docs/next-build-tasks.md)

## Product Positioning

对外不要包装成“自动回复外挂”或“平台消息抓取工具”。产品表达应保持为：

> 多平台电商客服 AI 助手，帮助商家识别客户问题、生成建议回复、降低重复客服工作量。

第一阶段默认人工确认发送，保留消息截图/证据和风险判断，避免产品过早进入高风险自动化形态。
