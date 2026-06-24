package com.aiagentchat.backend.message.service;

import com.aiagentchat.backend.message.dto.AiReplyRequest;
import com.aiagentchat.backend.message.dto.AiReplyResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiReplyService {

    private final RestClient restClient;

    public AiReplyService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8000")
                .build();
    }

    public AiReplyResponse requestSuggestion(AiReplyRequest request) {
        return restClient.post()
                .uri("/v1/reply/suggest")
                .body(request)
                .retrieve()
                .body(AiReplyResponse.class);
    }
}

