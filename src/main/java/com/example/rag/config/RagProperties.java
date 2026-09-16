package com.example.rag.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Type-safe configuration for the RAG pipeline. */
@Validated
@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        @Valid Mistral mistral,
        @Valid PgVector pgvector,
        @Valid Ingestion ingestion) {

    public record Mistral(
            String apiKey,
            @NotBlank String embeddingModelName,
            @NotBlank String chatModelName,
            Duration timeout) {

        @AssertTrue(message = "MISTRAL_AI_API_KEY must be set")
        public boolean isApiKeyConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record PgVector(
            @NotBlank String host,
            @Min(1) @Max(65535) int port,
            @NotBlank String database,
            @NotBlank String user,
            @NotBlank String password,
            @NotBlank String table,
            @Positive int dimension) {
    }

    public record Ingestion(
            @NotBlank String documentPath,
            @Positive int chunkSize,
            @Min(0) int chunkOverlap,
            boolean clearBeforeIngest) {

        @AssertTrue(message = "chunk-overlap must be smaller than chunk-size")
        public boolean isOverlapValid() {
            return chunkOverlap < chunkSize;
        }
    }
}
