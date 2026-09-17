package com.example.rag.web;

import com.example.rag.assistant.RagAssistant;
import com.example.rag.service.RagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final RagAssistant assistant;
    private final RagService ragService;

    public ChatController(RagAssistant assistant, RagService ragService) {
        this.assistant = assistant;
        this.ragService = ragService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String answer = assistant.chat(request.message().trim());
        return ResponseEntity.ok(new ChatResponse(answer, Instant.now()));
    }

    @GetMapping("/ingestion/status")
    public RagService.IngestionStatus ingestionStatus() {
        return ragService.status();
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RagService.IngestionStatus> uploadDocument(
            @RequestPart("file") MultipartFile file) {
        ragService.ingest(file);
        return ResponseEntity.ok(ragService.status());
    }

    public record ChatRequest(
            @NotBlank(message = "message is required")
            @Size(max = 4_000, message = "message must be at most 4000 characters")
            String message) {
    }

    public record ChatResponse(String answer, Instant timestamp) {
    }
}
