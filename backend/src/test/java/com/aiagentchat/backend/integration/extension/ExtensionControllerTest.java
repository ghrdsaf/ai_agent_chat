package com.aiagentchat.backend.integration.extension;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aiagentchat.backend.message.dto.AiReplyResponse;
import com.aiagentchat.backend.message.service.AiReplyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:extension-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "ai.service.base-url=http://ai-service.test"
})
class ExtensionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiReplyService aiReplyService;

    @BeforeEach
    void setUp() {
        when(aiReplyService.requestSuggestion(any()))
                .thenReturn(new AiReplyResponse(
                        "亲，这边马上帮您确认。",
                        "low",
                        0.8,
                        List.of(),
                        "human_confirm"));
    }

    @Test
    void resolvesPlatformAccount() throws Exception {
        mockMvc.perform(post("/api/extension/platform-accounts/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platform": "pdd",
                                  "shopName": "示例店铺",
                                  "accountName": "客服A",
                                  "pageUrl": "https://mms.pinduoduo.com/chat-merchant/index.html"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformAccountId").value("pdd:示例店铺:客服A"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void ingestsMessagesAndReturnsSuggestions() throws Exception {
        mockMvc.perform(post("/api/extension/messages/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformAccountId": "pdd:示例店铺:客服A",
                                  "platform": "pdd",
                                  "conversation": {
                                    "externalConversationKey": "conv_001",
                                    "buyerName": "买家昵称",
                                    "title": "当前会话"
                                  },
                                  "messages": [
                                    {
                                      "externalMessageKey": "msg_001",
                                      "direction": "customer",
                                      "messageType": "text",
                                      "text": "这个什么时候发货？",
                                      "occurredAt": null,
                                      "rawHtmlHash": "hash_001"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.duplicates").value(0))
                .andExpect(jsonPath("$.suggestions", hasSize(1)))
                .andExpect(jsonPath("$.suggestions[0].text").value("亲，这边马上帮您确认。"))
                .andExpect(jsonPath("$.suggestions[0].riskLevel").value("low"));
    }

    @Test
    void ignoresDuplicateMessagesIdempotently() throws Exception {
        String payload = """
                {
                  "platformAccountId": "pdd:示例店铺:客服A",
                  "platform": "pdd",
                  "conversation": {
                    "externalConversationKey": "conv_dup",
                    "buyerName": "买家昵称",
                    "title": "当前会话"
                  },
                  "messages": [
                    {
                      "externalMessageKey": "msg_dup",
                      "direction": "customer",
                      "messageType": "text",
                      "text": "物流到哪里了？"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/extension/messages/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1));

        mockMvc.perform(post("/api/extension/messages/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.duplicates").value(1));
    }

    @Test
    void generatesDirectReplySuggestion() throws Exception {
        mockMvc.perform(post("/api/extension/reply-suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platform": "pdd",
                                  "accountHint": {
                                    "shopName": "示例店铺",
                                    "accountName": "客服A"
                                  },
                                  "conversation": {
                                    "externalConversationKey": "conv_002",
                                    "buyerName": "买家昵称",
                                    "title": "当前会话"
                                  },
                                  "message": {
                                    "externalMessageKey": "msg_002",
                                    "direction": "customer",
                                    "messageType": "text",
                                    "text": "还有库存吗？"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageKey").value("msg_002"))
                .andExpect(jsonPath("$.text").value("亲，这边马上帮您确认。"))
                .andExpect(jsonPath("$.action").value("human_confirm"));
    }
}
