(function () {
  const PLATFORM = "pdd";
  const MESSAGE_ROOT_SELECTORS = [
    "[class*='message']",
    "[class*='chat']",
    "[class*='im']",
    "[class*='conversation']"
  ];
  const INPUT_SELECTORS = [
    "textarea",
    "[contenteditable='true']",
    "input[type='text']"
  ];

  function getContext() {
    return {
      url: location.href,
      title: document.title,
      document
    };
  }

  function isSupportedPage(context) {
    const url = context.url || "";
    const title = context.title || "";
    const hasInput = Boolean(findReplyInput(context));
    return url.includes("mms.pinduoduo.com") && (url.includes("chat") || title.includes("客服") || hasInput);
  }

  function getPlatformAccountHint(context) {
    const bodyText = safeText(context.document.body).slice(0, 800);
    const shopMatch = bodyText.match(/([\u4e00-\u9fa5A-Za-z0-9_-]{2,30}(店|旗舰店|专营店|商铺))/);
    return {
      shopName: shopMatch ? shopMatch[1] : "",
      accountName: "",
      rawText: bodyText.slice(0, 300)
    };
  }

  function getCurrentConversation(context) {
    const candidates = Array.from(context.document.querySelectorAll("[class*='active'], [class*='selected'], [class*='current']"));
    const activeText = candidates.map(safeText).find((text) => text && text.length <= 120) || "";
    const visibleMessages = extractVisibleMessages(context);
    const firstMessage = visibleMessages[0] ? visibleMessages[0].text || visibleMessages[0].rawHtmlHash : "";
    const rawKey = [PLATFORM, location.pathname, activeText, firstMessage].join("|");
    return {
      externalConversationKey: `${PLATFORM}_${window.AiAgentChatHash.stableHash(rawKey)}`,
      buyerName: activeText.split(/\s+/)[0] || "",
      title: activeText || document.title,
      rawText: activeText
    };
  }

  function extractVisibleMessages(context) {
    const roots = findMessageRoots(context.document);
    const nodes = roots.length > 0 ? roots.flatMap(findMessageLikeNodes) : findMessageLikeNodes(context.document.body);
    const seen = new Set();
    const messages = [];

    nodes.forEach((node) => {
      if (!isVisible(node)) {
        return;
      }

      const text = normalizeText(safeText(node));
      const imageUrls = extractImageUrls(node);
      const linkUrls = extractLinkUrls(node);
      if (!text && imageUrls.length === 0 && linkUrls.length === 0) {
        return;
      }
      if (text.length > 1000) {
        return;
      }

      const direction = detectDirection(node);
      const messageType = detectMessageType(text, imageUrls, linkUrls);
      const rawHtmlHash = window.AiAgentChatHash.stableHash(node.outerHTML || text);
      const externalMessageKey = `${PLATFORM}_${window.AiAgentChatHash.stableHash([
        location.pathname,
        direction,
        messageType,
        text,
        imageUrls.join(","),
        rawHtmlHash
      ].join("|"))}`;

      if (seen.has(externalMessageKey)) {
        return;
      }
      seen.add(externalMessageKey);

      messages.push({
        externalMessageKey,
        direction,
        messageType,
        text,
        imageUrls,
        linkUrls,
        occurredAt: null,
        rawHtmlHash,
        domPathHint: buildDomPathHint(node)
      });
    });

    return messages.slice(-20);
  }

  function findReplyInput(context) {
    for (const selector of INPUT_SELECTORS) {
      const candidates = Array.from(context.document.querySelectorAll(selector)).filter(isVisible);
      const editable = candidates.find((item) => {
        const role = item.getAttribute("role") || "";
        const aria = item.getAttribute("aria-label") || "";
        const placeholder = item.getAttribute("placeholder") || "";
        return role.includes("textbox")
          || aria.includes("输入")
          || placeholder.includes("输入")
          || item.tagName === "TEXTAREA"
          || item.isContentEditable;
      });
      if (editable) {
        return editable;
      }
    }
    return null;
  }

  async function fillReplyDraft(context, text) {
    const input = findReplyInput(context);
    if (!input) {
      throw new Error("未找到回复输入框");
    }

    input.focus();
    if (input.isContentEditable) {
      input.textContent = text;
    } else {
      input.value = text;
    }

    ["input", "change", "compositionend"].forEach((eventName) => {
      input.dispatchEvent(new Event(eventName, { bubbles: true }));
    });
  }

  function findMessageRoots(doc) {
    return MESSAGE_ROOT_SELECTORS.flatMap((selector) => Array.from(doc.querySelectorAll(selector))).filter((node) => {
      const text = safeText(node);
      return isVisible(node) && text.length > 0 && text.length < 4000;
    }).slice(0, 8);
  }

  function findMessageLikeNodes(root) {
    if (!root) {
      return [];
    }
    const selectors = [
      "[class*='bubble']",
      "[class*='msg']",
      "[class*='message']",
      "[class*='item']"
    ];
    return selectors.flatMap((selector) => Array.from(root.querySelectorAll(selector))).filter((node) => {
      const text = normalizeText(safeText(node));
      const imgs = node.querySelectorAll ? node.querySelectorAll("img").length : 0;
      return (text.length >= 1 && text.length <= 500) || imgs > 0;
    });
  }

  function detectDirection(node) {
    const text = [
      node.className || "",
      node.getAttribute("data-role") || "",
      node.getAttribute("class") || ""
    ].join(" ").toLowerCase();

    if (text.includes("right") || text.includes("self") || text.includes("seller") || text.includes("merchant") || text.includes("out")) {
      return "merchant";
    }
    if (text.includes("system") || text.includes("notice") || text.includes("time")) {
      return "system";
    }
    return "customer";
  }

  function detectMessageType(text, imageUrls, linkUrls) {
    if (imageUrls.length > 0 && text) {
      return "mixed";
    }
    if (imageUrls.length > 0) {
      return "image";
    }
    if (/订单|物流|退款|售后/.test(text)) {
      return "order_card";
    }
    if (/商品|规格|价格|库存|优惠/.test(text) || linkUrls.length > 0) {
      return "product_card";
    }
    return "text";
  }

  function extractImageUrls(node) {
    return Array.from(node.querySelectorAll ? node.querySelectorAll("img") : [])
      .map((img) => img.currentSrc || img.src || img.getAttribute("data-src") || img.getAttribute("srcset") || "")
      .filter(Boolean)
      .slice(0, 5);
  }

  function extractLinkUrls(node) {
    return Array.from(node.querySelectorAll ? node.querySelectorAll("a[href]") : [])
      .map((link) => link.href)
      .filter(Boolean)
      .slice(0, 5);
  }

  function buildDomPathHint(node) {
    const names = [];
    let current = node;
    while (current && current !== document.body && names.length < 5) {
      const className = String(current.className || "").split(/\s+/).filter(Boolean).slice(0, 2).join(".");
      names.unshift(`${current.tagName.toLowerCase()}${className ? `.${className}` : ""}`);
      current = current.parentElement;
    }
    return names.join(" > ");
  }

  function safeText(node) {
    return node ? node.innerText || node.textContent || "" : "";
  }

  function normalizeText(text) {
    return String(text || "").replace(/\s+/g, " ").trim();
  }

  function isVisible(node) {
    if (!node || !node.getBoundingClientRect) {
      return false;
    }
    const rect = node.getBoundingClientRect();
    const style = window.getComputedStyle(node);
    return rect.width > 0
      && rect.height > 0
      && rect.bottom >= 0
      && rect.top <= window.innerHeight
      && style.visibility !== "hidden"
      && style.display !== "none";
  }

  window.AiAgentChatPddAdapter = {
    platform: PLATFORM,
    getContext,
    isSupportedPage,
    getPlatformAccountHint,
    getCurrentConversation,
    extractVisibleMessages,
    findReplyInput,
    fillReplyDraft
  };
})();
