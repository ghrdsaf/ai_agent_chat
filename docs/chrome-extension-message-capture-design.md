# Chrome 插件消息抓取设计

## 目标

使用 Chrome 插件从 Web 商家客服工作台中抓取消息，再把标准化事件发送到后端，用于生成 AI 建议回复。

第一版使用 DOM 提取，不使用 YOLO/OCR。只有在后续桌面端或 Android 视觉自动化无法访问 DOM 时，才引入 YOLO/OCR。

## 范围

### MVP 范围

- 识别已支持的 Web 客服页面。
- 提取当前平台账号提示信息。
- 识别当前活跃会话。
- 提取可见买家消息。
- 在 DOM 可获得的情况下，提取文本、图片 URL、商品卡片和订单卡片文本。
- 对消息去重。
- 将标准化消息事件发送到 Spring Boot。
- 接收回复建议。
- 在插件 UI 中展示建议。
- 支持复制或填入草稿。

### MVP 不做

- 无人值守批量自动发送。
- 逆向隐藏平台 API。
- YOLO/OCR 屏幕识别。
- Android/ADB 控制。
- 微信个人号自动化。

## 插件架构

```text
Chrome 插件
  manifest.json
  background service worker
  content scripts
  platform adapters
  sidebar / popup UI
```

### Content Script

运行在商家客服工作台页面内部。

职责：

- 判断当前页面是否已支持。
- 加载对应平台适配器。
- 监听 DOM 变化。
- 提取新消息。
- 在商家点击操作时填入回复草稿。

### Background Service Worker

作为插件和后端之间的桥。

职责：

- 保存插件认证 Token。
- 调用 Spring Boot API。
- 管理重试和限流。
- 在 Content Script 和侧边栏/弹窗之间转发消息。

### 侧边栏 / 弹窗

职责：

- 展示当前会话状态。
- 展示 AI 建议回复。
- 展示风险等级和原因。
- 提供复制、填入草稿等操作。
- 展示不支持页面、登录失效、后端不可用等错误。

## 平台适配器契约

每个 Web 平台适配器应暴露相同结构：

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

建议共享类型：

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

## 消息抓取策略

### 1. 页面识别

Content Script 检查：

- 当前 URL 的 host/path。
- 已知页面标题特征。
- 稳定根节点是否存在。
- 聊天列表、消息列表或回复输入框是否存在。

示例：

```text
如果 URL 匹配拼多多商家工作台
并且聊天根节点存在
并且回复输入框存在
则加载 pdd-web-adapter
```

不要只依赖一个选择器。应使用一组弱信号组合判断，这样页面小改版时不容易完全失效。

### 2. 活跃会话识别

适配器需要生成稳定的 `externalConversationKey`。

优先来源：

1. DOM 属性中可见的会话 ID。
2. DOM 属性中可见的买家 ID 或用户 ID。
3. 工作台 URL 暴露的 query 参数。
4. 兜底方案：买家昵称 + 当前页面上下文 + 第一条可见消息做 Hash。

兜底方案 MVP 阶段可以接受，但需要标记为低置信度。

### 3. 消息提取

适配器扫描可见消息列表并提取消息节点。

对每个节点：

- 判断方向：买家、商家或系统。
- 使用 `innerText` 提取文本。
- 从 `img.src`、`data-src`、`srcset` 或预览链接提取图片。
- 从嵌套文本中提取商品卡片或订单卡片信息。
- 从 `a.href` 提取链接。
- 构造 `externalMessageKey`。

优先消息 Key：

```text
platform + externalConversationKey + platformMessageId
```

兜底消息 Key：

```text
platform + externalConversationKey + sha256(direction + messageType + text + imageUrls + rawHtmlHash)
```

### 4. DOM 变化监听

在消息列表根节点上使用 `MutationObserver`。

行为：

- DOM 变化后延迟 300-800 ms 再扫描，避免频繁触发。
- 每次变化后重新扫描可见消息。
- 只发送新发现的买家消息。
- 活跃会话变化时重新扫描。

默认不要高频轮询。可以保留 3-5 秒一次的轻量兜底轮询，用于异常恢复。

### 5. 消息去重

分两层去重。

插件内存层：

- 保存最近一批 `externalMessageKey` 的 LRU 集合。
- 活跃会话变化时重置。

后端层：

- 通过平台账号 + 外部会话 Key + 外部消息 Key 做唯一约束。
- 重复上报时幂等忽略。

### 6. 图片消息处理

PC Web 工作台中的图片消息不需要 YOLO。

优先处理方式：

- 从 DOM 中提取图片 URL 或 blob URL。
- 如果后端可直接访问 URL，则发送 URL。
- 如果图片只能在浏览器登录态中访问，则由插件 fetch 图片为 blob 后上传到后端。
- 后端把图片或对象存储 URL 交给多模态 AI 服务理解。

不要为了理解买家上传的图片而训练 YOLO，第一选择应该是多模态模型。

### 7. 填入回复草稿

Content Script 只在用户点击操作后填入平台回复框。

实现顺序：

1. 查找可编辑输入区：
   - `textarea`
   - `input`
   - `[contenteditable="true"]`
2. 聚焦输入区。
3. 设置 value 或 text content。
4. 派发 input/change/composition 事件，让页面框架感知内容变化。
5. MVP 阶段不点击发送按钮。

自动发送只能在后续低风险白名单意图中开放。

## 后端 API 形态

### 注册或解析平台账号

```http
POST /api/extension/platform-accounts/resolve
```

请求：

```json
{
  "platform": "pdd",
  "shopName": "示例店铺",
  "accountName": "客服A",
  "pageUrl": "https://..."
}
```

响应：

```json
{
  "platformAccountId": "pa_123",
  "status": "active"
}
```

### 消息入库

```http
POST /api/extension/messages/ingest
```

请求：

```json
{
  "platformAccountId": "pa_123",
  "platform": "pdd",
  "conversation": {
    "externalConversationKey": "conv_xxx",
    "buyerName": "买家昵称",
    "title": "当前会话"
  },
  "messages": [
    {
      "externalMessageKey": "msg_xxx",
      "direction": "customer",
      "messageType": "text",
      "text": "这个什么时候发货？",
      "occurredAt": null,
      "rawHtmlHash": "..."
    }
  ]
}
```

响应：

```json
{
  "accepted": 1,
  "duplicates": 0,
  "suggestions": [
    {
      "messageKey": "msg_xxx",
      "suggestionId": "rs_123",
      "text": "亲，这款商品正常会在24小时内发出，发出后会同步物流信息哦。",
      "riskLevel": "low",
      "action": "human_confirm"
    }
  ]
}
```

### 记录客服操作

```http
POST /api/extension/operator-actions
```

请求：

```json
{
  "suggestionId": "rs_123",
  "action": "fill_draft",
  "finalText": "亲，这款商品正常会在24小时内发出哦。"
}
```

## 数据模型补充

插件路线至少需要这些后端字段：

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

## 可靠性规则

- 不要假设平台 DOM 永远稳定。
- 选择器只放在平台适配器里。
- 优先使用多个弱信号，而不是一个脆弱选择器。
- 除非用户点击复制/填入，否则 DOM 提取只读。
- 记录提取失败时的平台、URL 特征和适配器版本。
- 保存足够排查错误回复的证据，但不要过度保存敏感数据。

## 安全与合规规则

- 只采集插件明确支持的页面。
- 只处理商家授权的平台账号。
- 不采集无关浏览数据。
- 不采集商家不可见的隐藏平台数据。
- 默认不自动发送。
- 插件 UI 必须提供清晰的暂停开关。

## MVP 实现顺序

1. 创建 `collectors/browser-extension`。
2. 添加 Manifest V3 插件骨架。
3. 添加 Background Service Worker 和 Content Script。
4. 添加适配器接口和 `pdd-web-adapter`。
5. 识别支持页面和活跃会话。
6. 提取可见文本消息。
7. 添加后端消息入库 API。
8. 添加 AI 建议调用。
9. 在侧边栏或弹窗中展示建议。
10. 添加复制和填入草稿动作。
11. 添加图片 URL 提取。
12. 为只能在浏览器登录态访问的图片增加上传路径。
