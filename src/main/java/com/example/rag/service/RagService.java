package com.example.rag.service;

import com.example.rag.config.RagProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/** Loads, chunks, embeds and persists the configured PDF document. */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagProperties properties;
    private final AtomicReference<IngestionStatus> status = new AtomicReference<>(IngestionStatus.notStarted());

    public RagService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            RagProperties properties) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.properties = properties;
    }

    /**
     * Ingests {@code src/main/resources/company_policy.pdf} by default. For a packaged
    * deployment, point {@code RAG_DOCUMENT_PATH} at an external PDF file.
     */
    public synchronized void ingest() {
        Path documentPath = Path.of(properties.ingestion().documentPath()).toAbsolutePath().normalize();
        Path loadPath = documentPath;
        boolean temporaryClasspathCopy = false;

        // In a packaged JAR, src/main/resources is no longer an OS directory.
        // Fall back to the bundled classpath resource so Render/Docker deployments work.
        if (!Files.isRegularFile(loadPath)) {
            ClassPathResource resource = new ClassPathResource("company_policy.pdf");
            if (!resource.exists()) {
                throw new IllegalStateException("RAG document does not exist: " + documentPath);
            }
            try {
                loadPath = Files.createTempFile("company_policy-", ".pdf");
                try (var inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, loadPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                temporaryClasspathCopy = true;
                documentPath = loadPath;
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Unable to prepare bundled RAG document", exception);
            }
        }

        status.set(IngestionStatus.running(documentPath.toString()));
        log.info("Ingesting RAG document: {}", documentPath);

        try {
            Document document = loadDocument(documentPath, new ApachePdfBoxDocumentParser());

            // This application owns the configured table. Replacing its contents makes
            // startup ingestion deterministic and prevents duplicate chunks after restarts.
            if (properties.ingestion().clearBeforeIngest()) {
                embeddingStore.removeAll();
            }

            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(
                            properties.ingestion().chunkSize(),
                            properties.ingestion().chunkOverlap()))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build()
                    .ingest(document);

            status.set(IngestionStatus.completed(documentPath.toString()));
            log.info("RAG document ingestion completed: {}", documentPath);
        } catch (RuntimeException exception) {
            status.set(IngestionStatus.failed(documentPath.toString(), exception.getMessage()));
            throw exception;
        } finally {
            if (temporaryClasspathCopy) {
                try {
                    Files.deleteIfExists(loadPath);
                } catch (java.io.IOException exception) {
                    log.warn("Unable to delete temporary RAG document {}", loadPath, exception);
                }
            }
        }
    }

    public IngestionStatus status() {
        return status.get();
    }

    public record IngestionStatus(String state, String document, Instant updatedAt, String error) {
        static IngestionStatus notStarted() {
            return new IngestionStatus("NOT_STARTED", null, Instant.now(), null);
        }

        static IngestionStatus running(String document) {
            return new IngestionStatus("RUNNING", document, Instant.now(), null);
        }

        static IngestionStatus completed(String document) {
            return new IngestionStatus("COMPLETED", document, Instant.now(), null);
        }

        static IngestionStatus failed(String document, String error) {
            return new IngestionStatus("FAILED", document, Instant.now(), error);
        }
    }
}
