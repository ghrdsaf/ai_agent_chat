(function () {
  const adapter = window.AiAgentChatPddAdapter;
  const recentKeys = new Set();
  let lastConversationKey = "";
  let observer = null;
  let scanTimer = null;

  function getState() {
    if (!adapter) {
      return {
        supported: false,
        platform: "",
        reason: "未加载平台适配器"
      };
    }

    const context = adapter.getContext();
    const supported = adapter.isSupportedPage(context);
    const conversation = supported ? adapter.getCurrentConversation(context) : null;
    const accountHint = supported ? adapter.getPlatformAccountHint(context) : null;
    const messages = supported ? adapter.extractVisibleMessages(context) : [];
    const latestCustomerMessage = [...messages].reverse().find((item) => item.direction === "customer") || null;

    return {
      supported,
      platform: adapter.platform,
      url: location.href,
      title: document.title,
      accountHint,
      conversation,
      messages,
      latestCustomerMessage,
      reason: supported ? "" : "当前页面暂未识别为已支持的客服工作台"
    };
  }

  function scheduleScan() {
    window.clearTimeout(scanTimer);
    scanTimer = window.setTimeout(scanAndNotify, 500);
  }

  function scanAndNotify() {
    const state = getState();
    if (!state.supported || !state.conversation) {
      notifyPopup(state);
      return;
    }

    if (state.conversation.externalConversationKey !== lastConversationKey) {
      recentKeys.clear();
      lastConversationKey = state.conversation.externalConversationKey;
    }

    const newMessages = state.messages.filter((message) => {
      if (message.direction !== "customer") {
        return false;
      }
      if (recentKeys.has(message.externalMessageKey)) {
        return false;
      }
      recentKeys.add(message.externalMessageKey);
      return true;
    });

    notifyPopup({
      ...state,
      newMessages
    });

    if (newMessages.length > 0) {
      chrome.runtime.sendMessage({
        type: "INGEST_MESSAGES",
        payload: {
          platform: state.platform,
          accountHint: state.accountHint,
          conversation: state.conversation,
          messages: newMessages,
          pageUrl: location.href
        }
      });
    }
  }

  function notifyPopup(payload) {
    chrome.runtime.sendMessage({
      type: "PAGE_STATE",
      payload
    }).catch(() => {
      // 弹窗未打开时忽略。
    });
  }

  function startObserver() {
    if (observer) {
      observer.disconnect();
    }
    observer = new MutationObserver(scheduleScan);
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true
    });
    scheduleScan();
    window.setInterval(scheduleScan, 5000);
  }

  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === "GET_PAGE_STATE") {
      sendResponse(getState());
      return true;
    }

    if (message.type === "FILL_DRAFT") {
      adapter.fillReplyDraft(adapter.getContext(), message.payload.text)
        .then(() => sendResponse({ ok: true }))
        .catch((error) => sendResponse({ ok: false, error: error.message }));
      return true;
    }

    return false;
  });

  startObserver();
})();
