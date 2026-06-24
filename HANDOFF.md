# HANDOFF

## RESUME PACKET

* Goal: 构建电脑端 Chrome 插件 + Spring Boot 后端 + Python AI 服务的电商客服 Copilot，先支持拼多多 Web 客服场景。
* Workflow State: Default mode；当前阶段=插件 MVP 联调；下一关=真实拼多多买家会话页验证；风险=中等，主要风险是平台 DOM/页面入口不确定。
* Branch: `main`；最新已推送提交：`5327690 chore: add local backend profile for plugin testing`；Dirty: no before this handoff.
* Next task: 在真实“买家聊天会话页”验证 content script 是否注入，并根据真实 DOM 校准 `pdd-web-adapter`。
* Verification: 打开真实客服会话页后，在页面控制台或 Chrome 控制中确认 `window.AiAgentChatPddAdapter === true`，插件弹窗能显示最新买家消息并可“填入草稿”。
* Read first: `HANDOFF.md`，`README.md`，`docs/chrome-extension-message-capture-design.md`，`collectors/browser-extension/README.md`，`collectors/browser-extension/adapters/pdd-web-adapter.js`。

## 当前目标

第一阶段产品是客服 Copilot，不是全自动外挂。插件负责读取当前 Web 客服页面可见消息、生成建议回复、复制或填入草稿；默认不自动点击发送。

## 已完成

- 建立项目路线文档：PC Chrome 插件优先，后端 Agent，商家知识库，后续再扩桌面/ADB。
- 新增 Chrome 插件 MVP：`collectors/browser-extension/`。
- 插件能力：
  - Manifest V3。
  - 拼多多 Web 适配器初版。
  - DOM 提取可见消息。
  - `MutationObserver` 监听页面变化。
  - Popup 展示平台、店铺、会话、最新买家消息、建议回复。
  - 支持复制和填入草稿。
  - 默认不自动发送。
- 新增后端插件 API：
  - `POST /api/extension/platform-accounts/resolve`
  - `POST /api/extension/messages/ingest`
  - `POST /api/extension/reply-suggestions`
- 后端复用现有 `conversation_mapping` 和 `message` 表做 MVP 入库。
- 新增 `application-local.yml`，本地测试用 H2 内存库，不依赖 PostgreSQL。
- AI 服务当前是 stub：`ai-service/app/main.py`，返回固定建议结构。
- 后端增加 AI 调用失败兜底，避免联调时插件链路中断。

## 验证结果

- 后端测试通过：
  - Command: `cd backend && mvn test`
  - Result: `Tests run: 4, Failures: 0, Errors: 0`
- 插件静态检查通过：
  - `manifest.json` 可解析。
  - `node --check` 通过：`background.js`、`content.js`、`pdd-web-adapter.js`、`popup.js`、`hash.js`。
- 本地服务曾成功启动：
  - AI 服务：`http://127.0.0.1:8000/healthz -> ok`
  - 后端：`http://127.0.0.1:8080/actuator/health -> UP`
- 后端消息入库接口曾成功返回：
  - `accepted=1`
  - 返回 `suggestions[0]`

## 当前浏览器测试发现

- 用户已安装插件，但当时打开的页面是：
  - `https://mms.pinduoduo.com/chat-service/robot?msfrom=mms_sidenav`
  - 页面标题：`拼多多 商家后台`
- 该页面不是实际买家聊天会话页，只是客服机器人/后台入口页。
- 在该页面检查结果：
  - `window.AiAgentChatHash === false`
  - `window.AiAgentChatPddAdapter === false`
  - 页面只有顶部搜索框，没有买家消息列表/客服回复输入框。
- 因此当时无法验证真实消息抓取。

## 重要决策

- 当前版本不自动发送消息，只支持“接收/识别消息 + 生成建议 + 复制/填入草稿”。
- 自动发送必须后置，并且只允许低风险白名单意图；退款、投诉、差评、赔偿、平台介入等必须人工处理。
- 图片消息第一阶段不训练 YOLO；Web 端优先从 DOM 取图片 URL/blob，后续交给多模态模型。
- 拼多多专用 SDK/Playwright 路线不作为主架构，只可参考业务能力。

## 运行命令

AI 服务：

```bash
cd ai-service
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

后端本地 H2 模式：

```bash
cd backend
mvn -DskipTests package
D:\java_home\jdk-17\bin\java.exe -jar target\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

插件安装目录：

```text
D:\workspace\product\ai_agent_chat\collectors\browser-extension
```

## 已知坑

- Windows 默认 `java` 可能命中 `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe`，启动 jar 异常且无日志；建议用 `D:\java_home\jdk-17\bin\java.exe`。
- PowerShell 控制台可能显示中文乱码，但 Markdown 文件本身是 UTF-8。
- 当前 `pdd-web-adapter` 是通用 DOM 识别初版，必须在真实买家会话页校准选择器。
- 如果 Chrome 页面没有注入插件，先确认扩展加载目录是否是 `collectors/browser-extension`，然后在 `chrome://extensions/` 点击插件“重新加载”，再刷新拼多多页面。

## 下一步唯一任务

打开真实拼多多买家聊天会话页，验证并校准插件注入和消息提取。

验收命令/检查：

```text
1. 页面中 window.AiAgentChatPddAdapter 存在。
2. 插件 Popup 显示“已识别客服工作台”。
3. 插件 Popup 显示最新买家消息。
4. 点击“重新生成”返回建议回复。
5. 点击“填入草稿”只写入输入框，不自动发送。
```
