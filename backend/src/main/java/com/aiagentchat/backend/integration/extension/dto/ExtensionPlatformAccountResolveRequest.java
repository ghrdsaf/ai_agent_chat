package com.aiagentchat.backend.integration.extension.dto;

import jakarta.validation.constraints.NotBlank;

public record ExtensionPlatformAccountResolveRequest(
        @NotBlank String platform,
        String shopName,
        String accountName,
        String pageUrl) {
}
