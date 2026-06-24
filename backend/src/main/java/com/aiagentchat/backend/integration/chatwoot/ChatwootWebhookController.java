package com.aiagentchat.backend.integration.chatwoot;

import com.aiagentchat.backend.integration.chatwoot.dto.ChatwootWebhookPayload;
import com.aiagentchat.backend.message.MessageIngestService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/chatwoot")
@RequiredArgsConstructor
public class ChatwootWebhookController {

    private final MessageIngestService messageIngestService;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Chatwoot-Signature", required = false) String signature,
            @RequestHeader(value = "X-Chatwoot-Timestamp", required = false) String timestamp,
            @Valid @RequestBody ChatwootWebhookPayload payload) {
        messageIngestService.handleChatwootWebhook(payload, signature, timestamp);
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}

