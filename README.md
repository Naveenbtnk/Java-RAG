# Java RAG — Spring Boot, LangChain4j, Mistral AI and PGVector

A complete Java 17+ Retrieval-Augmented Generation application. It parses a company-policy PDF, splits it into overlapping chunks, creates 1024-dimensional Mistral embeddings, stores them in PostgreSQL/PGVector, retrieves the five most relevant chunks, and sends the grounded prompt to Mistral AI. A responsive chat UI is included at `http://localhost:8080`.

## Architecture

```text
company_policy.pdf
       │ PDFBox → recursive chunks (500 chars, 50 overlap)
       ▼
Mistral mistral-embed → PGVector (rag_embeddings, 1024 dimensions)
                              │ top 5, score ≥ 0.6
Browser UI → POST /api/chat ──┴─→ @AiService → Mistral chat model → answer
```

## Prerequisites

- JDK 17 or newer (the Maven build targets Java 17)
- Docker Desktop
- Eclipse with Maven integration (m2e); the included Maven Wrapper also works from PowerShell
- A [Mistral AI API key](https://console.mistral.ai/)

## Run locally

1. Put the PDF you want to query at `src/main/resources/company_policy.pdf`.
2. Start PGVector:

   ```powershell
   docker compose up -d
   ```

3. Set the API key for the current PowerShell window:

   ```powershell
   $env:MISTRAL_AI_API_KEY = "your-key-here"
   ```

4. Start Spring Boot:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. Open `http://localhost:8080`, or call the REST API directly:

   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri http://localhost:8080/api/chat `
     -ContentType application/json `
     -Body '{"message":"What is the annual leave policy?"}'
   ```

At startup, the `CommandLineRunner` ingests the PDF. It clears the application-owned embedding table first, so restarts do not create duplicate chunks. Set `RAG_CLEAR_BEFORE_INGEST=false` only when an external ingestion process manages that table. The app fails fast when the API key, database, or PDF is missing instead of starting in a misleading half-working state.

## Eclipse setup

1. Use **File → Import → Maven → Existing Maven Projects**.
2. Select this folder (the one containing `pom.xml`).
3. Let Eclipse download dependencies.
4. Add `MISTRAL_AI_API_KEY` under **Run Configurations → Environment**.
5. Run `RagApplication.java` as **Spring Boot App** (or **Java Application**).

The screenshot's original POM is malformed and uses a Java 18 library; this project replaces it with a valid Spring Boot parent POM and Java 17 compiler target. A new Eclipse workspace is optional—importing this project is enough.

## Configuration

All settings have environment-variable overrides in `application.yml`:

| Variable | Default | Purpose |
|---|---:|---|
| `MISTRAL_AI_API_KEY` | required | Mistral authentication |
| `MISTRAL_EMBEDDING_MODEL` | `mistral-embed` | 1024-dimensional embedding model |
| `MISTRAL_CHAT_MODEL` | `mistral-small-latest` | Answer-generation model |
| `PGVECTOR_HOST` / `PGVECTOR_PORT` | `localhost` / `5433` | PostgreSQL address |
| `PGVECTOR_DATABASE` | `vector_db` | Database name |
| `PGVECTOR_USER` / `PGVECTOR_PASSWORD` | `postgres` / `postgres` | Local credentials |
| `RAG_DOCUMENT_PATH` | `src/main/resources/company_policy.pdf` | PDF to ingest |
| `RAG_CLEAR_BEFORE_INGEST` | `true` | Replace table contents before startup ingestion |

Do not commit `.env` or API keys. Use a secret manager and TLS-enabled database connections in production.

## Endpoints

- `POST /api/chat` — JSON body: `{"message":"..."}`
- `GET /api/ingestion/status` — current ingestion state
- `GET /actuator/health` — application health
- `GET /` — browser chat UI

## Useful commands

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
docker compose logs -f postgres
docker compose down
```
