# Chrome 插件开发提示词

下面这段提示词用于让后续 AI 继续开发本项目的 Chrome 插件。使用时可以直接复制给 AI。

```text
你是资深 Chrome 插件和电商客服系统工程师。请在当前仓库继续开发 `collectors/browser-extension` 插件。

项目目标：
- 做一个电脑端电商客服 AI Copilot。
- 第一阶段通过 Chrome 插件读取 Web 商家客服工作台里的可见买家消息。
- 不使用 YOLO/OCR，不逆向隐藏平台 API，不默认自动发送。
- 插件从 DOM 提取消息，发送到 Spring Boot 后端，后端调用 AI 服务生成建议回复。
- 后端不可用时，插件可以使用本地兜底建议，方便演示。

当前技术路线：
- Chrome Extension Manifest V3。
- Content Script 负责页面识别、DOM 监听、消息提取、填入草稿。
- Background Service Worker 负责配置、后端 API 调用、兜底建议。
- Popup UI 负责展示当前会话、最新买家消息、建议回复、复制和填入草稿。
- 第一平台是拼多多商家 Web 客服工作台，适配器文件是 `adapters/pdd-web-adapter.js`。

必须遵守：
- 所有可见文档和提示写中文，必要技术名词可以保留英文。
- 平台选择器只能放在平台适配器内部，不能泄漏到通用业务逻辑。
- 默认只读页面，只有用户点击“填入草稿”时才写入输入框。
- MVP 不点击发送按钮。
- 图片消息优先从 DOM 拿图片 URL 或 blob，交给多模态模型，不训练 YOLO。
- 新增能力要更新 `docs/chrome-extension-message-capture-design.md` 和 `collectors/browser-extension/README.md`。

优先开发顺序：
1. 提升 `pdd-web-adapter` 的页面识别准确率。
2. 改进消息方向判断：买家、商家、系统消息。
3. 增加消息去重和会话切换稳定性。
4. 增加图片 URL 提取和图片上传接口。
5. 对接真实后端 API：平台账号解析、消息入库、建议回复、客服操作记录。
6. 增加插件暂停开关和错误日志。
7. 再考虑抖店、快手、千牛 Web 适配器。

验收标准：
- 在 Chrome 扩展程序页面以“加载已解压的扩展程序”方式加载成功。
- 打开支持的客服工作台后，Popup 能识别页面。
- Popup 能展示最新买家消息。
- 点击“重新生成”能生成建议回复。
- 点击“复制”能复制建议回复。
- 点击“填入草稿”能把建议写入客服输入框，但不自动发送。
- 后端关闭时仍能用本地兜底建议演示。
```
