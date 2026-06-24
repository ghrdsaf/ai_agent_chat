const elements = {
  statusText: document.getElementById("statusText"),
  statusDot: document.getElementById("statusDot"),
  backendBaseUrl: document.getElementById("backendBaseUrl"),
  enableBackend: document.getElementById("enableBackend"),
  saveConfig: document.getElementById("saveConfig"),
  platformText: document.getElementById("platformText"),
  shopText: document.getElementById("shopText"),
  conversationText: document.getElementById("conversationText"),
  messageBox: document.getElementById("messageBox"),
  suggestionText: document.getElementById("suggestionText"),
  refreshSuggestion: document.getElementById("refreshSuggestion"),
  copySuggestion: document.getElementById("copySuggestion"),
  fillDraft: document.getElementById("fillDraft"),
  riskText: document.getElementById("riskText")
};

let currentState = null;
let latestSuggestion = null;

init();

async function init() {
  await loadConfig();
  await refreshPageState();
  bindEvents();
}

function bindEvents() {
  elements.saveConfig.addEventListener("click", saveConfig);
  elements.refreshSuggestion.addEventListener("click", requestSuggestion);
  elements.copySuggestion.addEventListener("click", copySuggestion);
  elements.fillDraft.addEventListener("click", fillDraft);

  chrome.runtime.onMessage.addListener((message) => {
    if (message.type === "PAGE_STATE") {
      currentState = message.payload;
      renderState(currentState);
    }
  });
}

async function loadConfig() {
  const config = await chrome.runtime.sendMessage({ type: "GET_CONFIG" });
  elements.backendBaseUrl.value = config.backendBaseUrl || "http://localhost:8080";
  elements.enableBackend.checked = Boolean(config.enableBackend);
}

async function saveConfig() {
  await chrome.runtime.sendMessage({
    type: "SAVE_CONFIG",
    payload: {
      backendBaseUrl: elements.backendBaseUrl.value.trim() || "http://localhost:8080",
      enableBackend: elements.enableBackend.checked
    }
  });
  elements.statusText.textContent = "配置已保存";
}

async function refreshPageState() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) {
    renderUnsupported("未找到当前标签页");
    return;
  }

  try {
    const state = await chrome.tabs.sendMessage(tab.id, { type: "GET_PAGE_STATE" });
    currentState = state;
    renderState(state);
    if (state?.latestCustomerMessage) {
      await requestSuggestion();
    }
  } catch (error) {
    renderUnsupported("当前页面未注入插件脚本，请确认打开的是已支持的商家客服页面");
  }
}

function renderState(state) {
  if (!state?.supported) {
    renderUnsupported(state?.reason || "当前页面暂不支持");
    return;
  }

  elements.statusDot.classList.add("ok");
  elements.statusText.textContent = "已识别客服工作台";
  elements.platformText.textContent = state.platform || "-";
  elements.shopText.textContent = state.accountHint?.shopName || "未识别";
  elements.conversationText.textContent = state.conversation?.title || state.conversation?.externalConversationKey || "-";

  const latest = state.latestCustomerMessage;
  if (latest) {
    elements.messageBox.classList.remove("empty");
    elements.messageBox.textContent = latest.text || `[${latest.messageType}]`;
  } else {
    elements.messageBox.classList.add("empty");
    elements.messageBox.textContent = "未读取到买家消息";
  }
}

function renderUnsupported(text) {
  elements.statusDot.classList.remove("ok");
  elements.statusText.textContent = text;
  elements.platformText.textContent = "-";
  elements.shopText.textContent = "-";
  elements.conversationText.textContent = "-";
  elements.messageBox.classList.add("empty");
  elements.messageBox.textContent = "未读取到买家消息";
}

async function requestSuggestion() {
  const message = currentState?.latestCustomerMessage;
  if (!message) {
    elements.statusText.textContent = "没有可生成建议的买家消息";
    return;
  }

  const response = await chrome.runtime.sendMessage({
    type: "REQUEST_SUGGESTION",
    payload: {
      platform: currentState.platform,
      accountHint: currentState.accountHint,
      conversation: currentState.conversation,
      message
    }
  });

  latestSuggestion = response.suggestion || response.suggestions?.[0] || null;
  if (latestSuggestion) {
    elements.suggestionText.value = latestSuggestion.text || latestSuggestion.suggestionText || "";
    elements.riskText.textContent = `风险：${latestSuggestion.riskLevel || "-"}，动作：${latestSuggestion.action || "human_confirm"}`;
    if (response.mode === "fallback") {
      elements.statusText.textContent = `后端不可用，已使用本地建议：${response.error}`;
    }
  }
}

async function copySuggestion() {
  const text = elements.suggestionText.value.trim();
  if (!text) {
    return;
  }
  await navigator.clipboard.writeText(text);
  elements.statusText.textContent = "建议回复已复制";
}

async function fillDraft() {
  const text = elements.suggestionText.value.trim();
  if (!text) {
    elements.statusText.textContent = "没有可填入的建议回复";
    return;
  }

  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) {
    elements.statusText.textContent = "未找到当前标签页";
    return;
  }

  const result = await chrome.tabs.sendMessage(tab.id, {
    type: "FILL_DRAFT",
    payload: { text }
  });

  elements.statusText.textContent = result?.ok ? "已填入草稿，发送前请人工确认" : `填入失败：${result?.error || "未知错误"}`;
}
