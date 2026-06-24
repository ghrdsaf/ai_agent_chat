# Chrome 插件采集器

这是 AI 客服助手的第一版 Chrome 插件 MVP。

## 当前能力

- 支持 Manifest V3。
- 在拼多多商家 Web 客服工作台页面注入 Content Script。
- 从 DOM 中提取可见买家消息。
- 通过 `MutationObserver` 监听页面变化。
- 在 Popup 中展示当前平台、店铺提示、会话、最新买家消息。
- 生成建议回复。
- 后端 API 未启用时，使用本地兜底建议。
- 支持复制建议回复。
- 支持把建议回复填入客服输入框。
- 默认不自动点击发送。

## 目录结构

```text
collectors/browser-extension/
  manifest.json
  background.js
  content.js
  adapters/
    pdd-web-adapter.js
  popup/
    popup.html
    popup.css
    popup.js
  shared/
    hash.js
```

## 安装方式

1. 打开 Chrome。
2. 访问 `chrome://extensions/`。
3. 打开右上角“开发者模式”。
4. 点击“加载已解压的扩展程序”。
5. 选择本目录：

```text
D:\workspace\product\ai_agent_chat\collectors\browser-extension
```

6. 浏览器右上角会出现“AI 客服助手 Copilot”插件。

## 使用方式

1. 打开拼多多商家 Web 客服工作台页面。
2. 进入一个买家会话。
3. 点击浏览器右上角插件图标。
4. 插件会显示：
   - 当前识别状态；
   - 平台；
   - 店铺提示；
   - 当前会话；
   - 最新买家消息；
   - 建议回复。
5. 点击“重新生成”可以重新生成建议。
6. 点击“复制”可以复制建议回复。
7. 点击“填入草稿”会把建议写入客服输入框。
8. 发送前请人工确认。

## 后端配置

Popup 中可以配置：

- 后端地址，默认 `http://localhost:8080`。
- 是否启用后端 API。

MVP 默认不启用后端 API。这样即使后端还没实现插件接口，也可以先演示：

- 页面识别；
- 消息提取；
- 本地兜底建议；
- 复制；
- 填入草稿。

启用后端 API 后，插件会尝试调用：

```text
POST /api/extension/platform-accounts/resolve
POST /api/extension/messages/ingest
POST /api/extension/reply-suggestions
```

这些接口需要后端后续补齐。

## 重要限制

- 当前拼多多适配器是通用 DOM 识别版，真实页面上可能需要根据页面结构继续调选择器。
- 当前不会自动发送消息。
- 当前不使用 YOLO/OCR。
- 当前不处理 Android/ADB。
- 当前不建议用于微信个人号。

## 调试方式

### 查看 Popup 日志

1. 打开 `chrome://extensions/`。
2. 找到本插件。
3. 点击“检查视图：弹出式窗口”。

### 查看 Content Script 日志

1. 打开客服工作台页面。
2. 按 F12 打开开发者工具。
3. 在 Console 中查看页面脚本输出。

### 重新加载插件

修改插件代码后：

1. 打开 `chrome://extensions/`。
2. 找到本插件。
3. 点击刷新按钮。
4. 刷新客服工作台页面。

## 下一步

1. 在真实拼多多客服工作台上校准 DOM 选择器。
2. 补齐后端插件 API。
3. 增加图片消息上传。
4. 增加插件暂停开关。
5. 增加错误日志和适配器版本。
6. 根据用户需求扩展抖店、快手、千牛 Web 适配器。
