package com.example.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.mistralai.MistralAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the language model, embedding model, vector store and retriever. */
@Configuration
public class RagConfig {

    @Bean
    public ChatModel mistralChatModel(RagProperties properties) {
        RagProperties.Mistral mistral = properties.mistral();
        return MistralAiChatModel.builder()
                .apiKey(mistral.apiKey())
                .modelName(mistral.chatModelName())
                .timeout(mistral.timeout())
                .build();
    }

    @Bean
    public EmbeddingModel mistralEmbeddingModel(RagProperties properties) {
        RagProperties.Mistral mistral = properties.mistral();
        return MistralAiEmbeddingModel.builder()
                .apiKey(mistral.apiKey())
                .modelName(mistral.embeddingModelName())
                .timeout(mistral.timeout())
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(RagProperties properties) {
        RagProperties.PgVector pg = properties.pgvector();
        return PgVectorEmbeddingStore.builder()
                .host(pg.host())
                .port(pg.port())
                .database(pg.database())
                .user(pg.user())
                .password(pg.password())
                .table(pg.table())
                .dimension(pg.dimension())
                .createTable(true)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel mistralEmbeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(mistralEmbeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .build();
    }
}
