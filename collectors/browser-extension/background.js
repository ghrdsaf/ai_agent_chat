const DEFAULT_CONFIG = {
  backendBaseUrl: "http://localhost:8080",
  enableBackend: false
};

chrome.runtime.onInstalled.addListener(async () => {
  const existing = await chrome.storage.sync.get(Object.keys(DEFAULT_CONFIG));
  await chrome.storage.sync.set({
    ...DEFAULT_CONFIG,
    ...existing
  });
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === "GET_CONFIG") {
    chrome.storage.sync.get(Object.keys(DEFAULT_CONFIG)).then((config) => {
      sendResponse({
        ...DEFAULT_CONFIG,
        ...config
      });
    });
    return true;
  }

  if (message.type === "SAVE_CONFIG") {
    chrome.storage.sync.set(message.payload || {}).then(() => {
      sendResponse({ ok: true });
    });
    return true;
  }

  if (message.type === "INGEST_MESSAGES") {
    handleIngest(message.payload).then(sendResponse);
    return true;
  }

  if (message.type === "REQUEST_SUGGESTION") {
    handleSuggestion(message.payload).then(sendResponse);
    return true;
  }

  return false;
});

async function handleIngest(payload) {
  const config = await getConfig();
  if (!config.enableBackend) {
    return {
      ok: true,
      mode: "local",
      suggestions: buildLocalSuggestions(payload.messages || [])
    };
  }

  try {
    const account = await resolvePlatformAccount(config, payload);
    const result = await postJson(`${config.backendBaseUrl}/api/extension/messages/ingest`, {
      platformAccountId: account.platformAccountId,
      platform: payload.platform,
      conversation: payload.conversation,
      messages: payload.messages || []
    });
    return {
      ok: true,
      mode: "backend",
      ...result
    };
  } catch (error) {
    return {
      ok: false,
      mode: "fallback",
      error: error.message,
      suggestions: buildLocalSuggestions(payload.messages || [])
    };
  }
}

async function handleSuggestion(payload) {
  const message = payload?.message;
  if (!message) {
    return {
      ok: false,
      error: "缺少消息内容"
    };
  }

  const config = await getConfig();
  if (!config.enableBackend) {
    return {
      ok: true,
      mode: "local",
      suggestion: buildLocalSuggestion(message)
    };
  }

  try {
    const result = await postJson(`${config.backendBaseUrl}/api/extension/reply-suggestions`, payload);
    return {
      ok: true,
      mode: "backend",
      suggestion: result
    };
  } catch (error) {
    return {
      ok: false,
      mode: "fallback",
      error: error.message,
      suggestion: buildLocalSuggestion(message)
    };
  }
}

async function resolvePlatformAccount(config, payload) {
  return postJson(`${config.backendBaseUrl}/api/extension/platform-accounts/resolve`, {
    platform: payload.platform,
    shopName: payload.accountHint?.shopName || "",
    accountName: payload.accountHint?.accountName || "",
    pageUrl: payload.pageUrl || ""
  });
}

async function getConfig() {
  const config = await chrome.storage.sync.get(Object.keys(DEFAULT_CONFIG));
  return {
    ...DEFAULT_CONFIG,
    ...config
  };
}

async function postJson(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    throw new Error(`后端请求失败：${response.status}`);
  }

  return response.json();
}

function buildLocalSuggestions(messages) {
  return messages.map((message) => ({
    messageKey: message.externalMessageKey,
    suggestionId: `local_${message.externalMessageKey}`,
    text: buildLocalReplyText(message.text || ""),
    riskLevel: detectRiskLevel(message.text || ""),
    action: "human_confirm"
  }));
}

function buildLocalSuggestion(message) {
  return {
    messageKey: message.externalMessageKey,
    suggestionId: `local_${message.externalMessageKey}`,
    text: buildLocalReplyText(message.text || ""),
    riskLevel: detectRiskLevel(message.text || ""),
    action: "human_confirm"
  };
}

function buildLocalReplyText(text) {
  if (/发货|什么时候发|多久发/.test(text)) {
    return "亲，这款商品正常会尽快安排发出，具体时效以页面和订单信息为准哦。";
  }
  if (/物流|快递|到哪|没收到/.test(text)) {
    return "亲，我帮您看一下物流情况，请您稍等一下。";
  }
  if (/退|退款|退货|售后/.test(text)) {
    return "亲，售后问题我先帮您核实订单情况，确认后给您准确处理方案。";
  }
  if (/有货|库存|能拍|能买吗/.test(text)) {
    return "亲，如果页面可以正常下单，一般就是有库存的哦。";
  }
  if (/尺码|大小|规格|颜色/.test(text)) {
    return "亲，您可以把需要的规格发我，我帮您确认一下是否合适。";
  }
  return "亲，您好，我看到了您的问题，这边马上帮您确认一下。";
}

function detectRiskLevel(text) {
  if (/投诉|差评|赔偿|报警|起诉|平台介入|退款|退货/.test(text)) {
    return "high";
  }
  if (/物流|发货|售后/.test(text)) {
    return "medium";
  }
  return "low";
}
