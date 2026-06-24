package com.aiagentchat.backend.integration.chatwoot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatwootWebhookPayload {

    @NotBlank
    private String event;

    @JsonProperty("event_name")
    private String eventName;

    @NotNull
    private Long id;

    @JsonProperty("content")
    private String content;

    @JsonProperty("message_type")
    private String messageType;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    private Conversation conversation;

    private Sender sender;

    private Map<String, Object> meta;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Conversation {
        private Long id;

        @JsonProperty("inbox_id")
        private Long inboxId;

        @JsonProperty("contact_inbox")
        private ContactInbox contactInbox;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactInbox {
        @JsonProperty("source_id")
        private String sourceId;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {
        private Long id;
        private String name;
        private String identifier;
    }
}

