# ai-service

Python AI 服务，负责为客服场景提供智能能力。

## 职责

- 买家意图识别。
- 商家知识库和 FAQ 检索。
- 建议回复生成。
- 风险等级判断。
- 返回证据和原因码，方便人工复核和调试。
- 后续可接入更复杂的 Agent 编排。

## 本地运行

```bash
uvicorn app.main:app --reload --port 8000
```

## 与后端关系

Spring Boot 后端负责业务数据和流程控制，`ai-service` 只负责 AI 推理能力。AI 服务不要直接操作平台账号、会话状态或发送动作。
