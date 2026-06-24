package com.aiagentchat.backend.message.service;

import com.aiagentchat.backend.message.dto.AiReplyRequest;
import com.aiagentchat.backend.message.dto.AiReplyResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiReplyService {

    private final RestClient restClient;

    public AiReplyService(RestClient.Builder builder, @Value("${ai.service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public AiReplyResponse requestSuggestion(AiReplyRequest request) {
        Map<String, Object> body = Map.of(
                "messageId", request.messageId() == null ? "" : request.messageId(),
                "channelType", request.channelType() == null ? "" : request.channelType(),
                "productId", request.productId() == null ? "" : request.productId(),
                "messageText", request.messageText() == null ? "" : request.messageText());
        return restClient.post()
                .uri("/v1/reply/suggest")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AiReplyResponse.class);
    }
}
