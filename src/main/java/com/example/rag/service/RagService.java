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
import org.springframework.web.multipart.MultipartFile;
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

        try {
            ingestPath(documentPath, documentPath.toString());
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

    /** Ingests a user-uploaded PDF and replaces the current knowledge base. */
    public synchronized void ingest(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("A non-empty PDF file is required");
        }
        String filename = upload.getOriginalFilename() == null ? "uploaded.pdf" : upload.getOriginalFilename();
        if (!filename.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile("uploaded-policy-", ".pdf");
            upload.transferTo(temporary);
            byte[] signature = new byte[5];
            try (var inputStream = Files.newInputStream(temporary)) {
                if (inputStream.read(signature) != signature.length
                        || !java.util.Arrays.equals(signature, "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                    throw new IllegalArgumentException("The uploaded file is not a valid PDF");
                }
            }
            ingestPath(temporary, filename);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to read uploaded PDF", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (java.io.IOException exception) {
                    log.warn("Unable to delete uploaded PDF temporary file {}", temporary, exception);
                }
            }
        }
    }

    private void ingestPath(Path documentPath, String displayName) {
        status.set(IngestionStatus.running(displayName));
        log.info("Ingesting RAG document: {}", displayName);
        try {
            Document document = loadDocument(documentPath, new ApachePdfBoxDocumentParser());
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
            status.set(IngestionStatus.completed(displayName));
            log.info("RAG document ingestion completed: {}", displayName);
        } catch (RuntimeException exception) {
            status.set(IngestionStatus.failed(displayName, exception.getMessage()));
            throw exception;
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
