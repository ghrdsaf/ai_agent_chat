# pdd-collector

拼多多采集器目录。

## 第一阶段职责

- 复用登录态
- 扫描会话列表
- 抽取最新消息
- 上报到 Chatwoot API Inbox

## 推荐技术栈

- `Playwright`
- `TypeScript` 或 `Python`

## 输出协议

采集器不直接参与业务决策，只负责把消息标准化后发出。

### 上报到 Spring Boot 的结构

```json
{
  "channel": "pinduoduo",
  "accountId": "shop-001",
  "externalSessionId": "pdd-session-001",
  "externalUserId": "buyer-001",
  "senderType": "customer",
  "messageType": "text",
  "messageText": "这个怎么安装",
  "messageTime": "2026-06-24T20:40:00+08:00",
  "rawPayload": {
    "source": "playwright"
  }
}
```

### 更推荐的方式

直接由采集器推送到 Chatwoot API Inbox：

1. 按平台用户创建/查询 contact
2. 维护 external session 和 chatwoot conversation 的映射
3. 追加 incoming message

## 降级要求

- 登录失效后停止采集并报警
- 页面结构变化时保存截图和 HTML 快照
- 未完成抽取的消息不得直接触发自动回复

