# PC Chrome 插件路线图

## 目标

把第一个商业 MVP 做成电脑端浏览器 AI 客服 Copilot。

第一版要帮助商家更快回复消息，同时避免一开始就引入 Android 设备、模拟器、ADB、OCR 模型训练或高风险无人值守自动化。

## 产品范围

### MVP 范围

- 识别已支持的商家客服页面。
- 读取当前会话中可见的最新买家消息。
- 将标准化后的消息内容发送到后端。
- 基于商家知识库生成建议回复。
- 在 Chrome 插件侧边栏或弹窗中展示建议。
- 支持一键复制和一键填入草稿。
- 保存消息、建议、风险等级和用户操作。

### MVP 不做

- 无人值守批量自动回复。
- 微信个人号自动化。
- Android/ADB 设备控制。
- YOLO/OCR 视觉识别。
- 逆向隐藏平台 API。
- 多平台同时自动化。

## 技术栈

### Chrome 插件

- Manifest V3。
- Content Script 负责 DOM 提取。
- 侧边栏或弹窗展示建议回复。
- Background Service Worker 负责认证 Token、API 调用和平台路由。
- 平台适配器模块示例：
  - `pdd-web-adapter`
  - `douyin-web-adapter`
  - `kuaishou-web-adapter`
  - `qianniu-web-adapter`

### 后端

- Spring Boot。
- PostgreSQL 或 MySQL 作为主数据库。
- 后续用 Redis 做限流、队列和短期会话状态。
- 为 Chrome 插件和管理后台提供 REST API。

### AI 服务

- Python FastAPI。
- 意图识别。
- 知识库检索。
- 回复生成。
- 风险分类。
- 返回证据，方便调试和人工复核。

### 知识库

知识库应支持：

- 商家 FAQ。
- 商品规则。
- 发货规则。
- 退换货规则。
- 回复语气和风格。
- 禁止承诺事项。
- 人工接管规则。

## 数据流

```text
浏览器中可见的买家消息
  -> Content Script 提取消息
  -> 插件标准化平台事件
  -> Spring Boot 保存消息
  -> Spring Boot 请求 AI 建议
  -> Python AI 服务检索知识库并生成回复
  -> Spring Boot 保存建议和风险结果
  -> 插件展示建议
  -> 商家复制或填入草稿
  -> 后端记录操作
```

## 平台适配器形态

每个浏览器平台适配器应实现：

```text
isSupportedPage(url, document)
getPlatformAccountHint(document)
getCurrentConversationKey(document)
extractLatestCustomerMessages(document)
findReplyInput(document)
fillReplyDraft(document, text)
```

不要让平台特定的 DOM 选择器泄漏到业务逻辑里。DOM 选择器只应该放在对应平台适配器内部。

## 后端领域模型

最小实体：

- `merchant`
- `platform_account`
- `conversation`
- `message`
- `reply_suggestion`
- `knowledge_document`
- `risk_decision`
- `operator_action`

关键字段：

- 平台名称。
- 外部会话 Key。
- 消息方向。
- 原始提取文本。
- 标准化文本。
- 置信度或提取来源。
- 建议回复文本。
- 风险等级。
- 是否人工确认。
- 是否已复制或填入草稿。

## 风险策略

默认行为：

- AI 可以生成建议。
- 人工确认。
- 默认关闭自动发送。

高风险意图必须人工处理：

- 退款纠纷。
- 赔付承诺。
- 差评威胁。
- 辱骂或骚扰。
- 平台规则争议。
- 法务或发票问题。
- 缺少订单上下文但需要订单核验的问题。

## 商业验证指标

从 MVP 开始就记录：

- 日活商家数。
- 每个商家每天生成的建议数。
- 复制/填入比例。
- 人工编辑比例。
- 建议被拒绝比例。
- 平均响应时间改善。
- 最高频重复意图。
- 从无法回答问题中发现的知识库缺口。

这些指标决定下一步是否扩更多平台、桌面自动化或 Android/ADB。

## 扩展路径

### 阶段 1：拼多多 Web Copilot

先把一个平台做好，默认保留人工确认。

### 阶段 2：知识库和质量优化

增加更好的 FAQ 检索、商家语气配置、高风险识别和建议复核。

### 阶段 3：更多 Web 平台

根据真实客户需求，增加抖店、快手、千牛 Web 适配器。

### 阶段 4：PC 桌面助手

对无法用浏览器插件处理的原生桌面客户端，再使用 Windows UI Automation 或截图/OCR。

### 阶段 5：Android/ADB 视觉适配器

等 PC 路线有付费用户后，再加入 ADB 截图、OCR、YOLO 和模拟输入，用于仅有 App 或移动端优先的平台。
