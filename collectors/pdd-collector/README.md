# pdd-collector

拼多多采集器历史占位目录。

## 当前状态

第一阶段路线已经调整为 **Chrome 插件 + Web 客服工作台**。因此新的拼多多 Web 采集实现应优先放到：

```text
collectors/browser-extension/
```

并以 `pdd-web-adapter` 的形式存在。

## 保留原因

该目录暂时保留，用于记录早期曾考虑过的拼多多专用采集器方向。后续如果确认不再需要，可以删除或迁移为浏览器插件适配器文档。

## 新实现应满足

- 使用 DOM 提取当前会话可见消息。
- 不依赖拼多多隐藏接口。
- 不默认自动发送。
- 支持复制和填入草稿。
- 通过后端统一 API 入库。

## 参考文档

- [Chrome 插件消息抓取设计](../../docs/chrome-extension-message-capture-design.md)
- [PC Chrome 插件路线图](../../docs/pc-chrome-extension-roadmap.md)
