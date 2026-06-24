package com.aiagentchat.backend.integration.extension;

import com.aiagentchat.backend.integration.extension.dto.ExtensionMessagesIngestRequest;
import com.aiagentchat.backend.integration.extension.dto.ExtensionMessagesIngestResponse;
import com.aiagentchat.backend.integration.extension.dto.ExtensionPlatformAccountResolveRequest;
import com.aiagentchat.backend.integration.extension.dto.ExtensionPlatformAccountResolveResponse;
import com.aiagentchat.backend.integration.extension.dto.ExtensionReplySuggestionRequest;
import com.aiagentchat.backend.integration.extension.dto.ExtensionSuggestionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extension")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExtensionController {

    private final ExtensionIngestService extensionIngestService;

    @PostMapping("/platform-accounts/resolve")
    public ExtensionPlatformAccountResolveResponse resolvePlatformAccount(
            @Valid @RequestBody ExtensionPlatformAccountResolveRequest request) {
        return extensionIngestService.resolvePlatformAccount(request);
    }

    @PostMapping("/messages/ingest")
    public ExtensionMessagesIngestResponse ingestMessages(
            @Valid @RequestBody ExtensionMessagesIngestRequest request) {
        return extensionIngestService.ingestMessages(request);
    }

    @PostMapping("/reply-suggestions")
    public ExtensionSuggestionDto suggestReply(
            @Valid @RequestBody ExtensionReplySuggestionRequest request) {
        return extensionIngestService.suggestReply(request);
    }
}
