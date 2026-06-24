# collectors

采集器目录用于放置不同平台和不同终端形态的消息采集适配器。

当前第一优先级是 Chrome 插件，不是 Android/ADB 采集器。

## 优先级

1. `browser-extension/`：面向 Web 商家客服工作台的 Chrome 插件。
2. `desktop-assistant/`：后续用于原生 PC 客户端的 Windows 桌面助手。
3. `android-adb/`：后续用于仅有 App 或移动端优先平台的 Android/ADB 视觉采集器。

## 采集器职责

- 识别已支持的平台和账号上下文。
- 提取可见买家消息。
- 将平台特定数据标准化为后端消息模型。
- 把 AI 建议展示回 UI 层。
- 执行保守操作，例如复制或填入草稿。
- 上报客服操作和异常。

## 第一版不做

- 无人值守自动回复。
- 逆向隐藏 API。
- Android 模拟器或真机接入。
- 微信个人号自动化。
