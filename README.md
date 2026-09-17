# 🧠 Java RAG — Spring Boot · LangChain4j · Mistral AI · PGVector

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)
![LangChain4j](https://img.shields.io/badge/LangChain4j-RAG-blueviolet)
![Mistral AI](https://img.shields.io/badge/Mistral%20AI-Embeddings%20%26%20Chat-FF7000)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PGVector-336791?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

A complete Java Retrieval-Augmented Generation (RAG) application. It parses a company-policy PDF, splits it into overlapping chunks, creates 1024-dimensional Mistral embeddings, stores them in PostgreSQL/PGVector, retrieves the five most relevant chunks per query, and sends a grounded prompt to Mistral AI. A responsive chat UI ships out of the box at `http://localhost:8080`.

---

## 🌐 Live Demo

| | Link |
|---|---|
| 🚀 Application | [Open Java RAG](https://java-rag.onrender.com/) |
| ❤️ Health Check | [Application health](https://java-rag.onrender.com/actuator/health) |

---

## 📚 Table of Contents

- [Live Demo](#-live-demo)
- [Features](#-features)
- [Architecture](#️-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Eclipse Setup](#-eclipse-setup)
- [Configuration](#️-configuration)
- [API Endpoints](#-api-endpoints)
- [Useful Commands](#-useful-commands)
- [Security Notes](#-security-notes)
- [Troubleshooting](#-troubleshooting)
- [License](#-license)

---

## ✨ Features

- 📄 **PDF ingestion** — automatic parsing and recursive chunking (500 chars, 50 overlap) via PDFBox
- 🧬 **Semantic embeddings** — 1024-dimensional vectors from Mistral's `mistral-embed`
- 🔍 **Vector similarity search** — top-5 retrieval with a 0.6 minimum score threshold via PGVector
- 💬 **Grounded chat** — answers generated only from retrieved context, reducing hallucination
- 🖥️ **Built-in UI** — responsive chat interface, no separate frontend needed
- 🔌 **REST API** — drop-in `/api/chat` endpoint for integration with other services
- ♻️ **Idempotent startup** — clears and re-ingests the embedding table on each run (configurable)
- 🛡️ **Fail-fast checks** — refuses to start with a misleading half-working state if the API key, database, or PDF is missing

---

## 🏗️ Architecture

```text
company_policy.pdf
       │  PDFBox → recursive chunks (500 chars, 50 overlap)
       ▼
Mistral mistral-embed → PGVector (rag_embeddings, 1024 dimensions)
                              │  top 5 matches, score ≥ 0.6
Browser UI → POST /api/chat ──┴──→ @AiService → Mistral chat model → answer
```

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17+ |
| Framework | Spring Boot |
| RAG Orchestration | LangChain4j (`@AiService`) |
| LLM Provider | Mistral AI (embeddings + chat) |
| Vector Store | PostgreSQL + PGVector |
| PDF Parsing | Apache PDFBox |
| Local Infra | Docker Compose |

---

## ✅ Prerequisites

| | Requirement |
|---|---|
| ☕ | JDK 17 or newer (the Maven build targets Java 17) |
| 🐳 | Docker Desktop |
| 🧩 | Eclipse with Maven integration (m2e) — the included Maven Wrapper also works standalone from PowerShell |
| 🔑 | A [Mistral AI API key](https://console.mistral.ai/) |

---

## 🚀 Quick Start

1. **Add your document**
   Place the PDF you want to query at `src/main/resources/company_policy.pdf`.

2. **Start PGVector**

   ```powershell
   docker compose up -d
   ```

3. **Set your API key** (current PowerShell session)

   ```powershell
   $env:MISTRAL_AI_API_KEY = "your-key-here"
   ```

4. **Start the application**

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

5. **Try it out**
   Open `http://localhost:8080` in your browser, or call the REST API directly:

   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri http://localhost:8080/api/chat `
     -ContentType application/json `
     -Body '{"message":"What is the annual leave policy?"}'
   ```

> ℹ️ At startup, the `CommandLineRunner` ingests the PDF and clears the application-owned embedding table first, so restarts don't create duplicate chunks. Set `RAG_CLEAR_BEFORE_INGEST=false` only when an external ingestion process manages that table.

---

## 🖥️ Eclipse Setup

1. **File → Import → Maven → Existing Maven Projects**
2. Select this folder (the one containing `pom.xml`)
3. Let Eclipse download dependencies
4. Add `MISTRAL_AI_API_KEY` under **Run Configurations → Environment**
5. Run `RagApplication.java` as a **Spring Boot App** (or **Java Application**)

> 📝 The screenshot's original POM is malformed and depends on a Java 18 library. This project replaces it with a valid Spring Boot parent POM and a Java 17 compiler target. A new Eclipse workspace is optional — simply importing this project is enough.

---

## ⚙️ Configuration

All settings have environment-variable overrides in `application.yml`:

| Variable | Default | Purpose |
|---|---:|---|
| `MISTRAL_AI_API_KEY` | *required* | Mistral authentication |
| `MISTRAL_EMBEDDING_MODEL` | `mistral-embed` | 1024-dimensional embedding model |
| `MISTRAL_CHAT_MODEL` | `mistral-small-latest` | Answer-generation model |
| `PGVECTOR_HOST` / `PGVECTOR_PORT` | `localhost` / `5433` | PostgreSQL address |
| `PGVECTOR_DATABASE` | `vector_db` | Database name |
| `PGVECTOR_USER` / `PGVECTOR_PASSWORD` | `postgres` / `postgres` | Local credentials |
| `RAG_DOCUMENT_PATH` | `src/main/resources/company_policy.pdf` | PDF to ingest |
| `RAG_CLEAR_BEFORE_INGEST` | `true` | Replace table contents before startup ingestion |

---

## 📡 API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/chat` | Send `{"message":"..."}`, receive a grounded answer |
| `POST` | `/api/documents` | Upload a PDF using multipart field `file` and replace the indexed policy |
| `GET` | `/api/ingestion/status` | Check current ingestion state |
| `GET` | `/actuator/health` | Application health check |
| `GET` | `/` | Browser chat UI |

---

## 🛠️ Useful Commands

```powershell
.\mvnw.cmd test              # run tests
.\mvnw.cmd clean package     # build a deployable jar
docker compose logs -f postgres   # tail database logs
docker compose down          # stop and remove containers
```

---

## 🔒 Security Notes

- 🚫 Never commit `.env` files or API keys to version control
- 🔐 Use a secret manager (e.g., Vault, AWS Secrets Manager, Azure Key Vault) in production
- 🔒 Enable TLS on all production database connections
- 🧾 Rotate the `MISTRAL_AI_API_KEY` periodically and scope it to least privilege
- 👤 Protect `/api/documents` with authentication, malware scanning, and tenant isolation before accepting uploads from untrusted users

---

## 🐛 Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| App fails to start immediately | Missing `MISTRAL_AI_API_KEY`, unreachable database, or missing PDF | The app fails fast by design — check the startup log for which precondition failed |
| Connection refused to PGVector | Docker container not running or wrong port | Run `docker compose up -d` and confirm `PGVECTOR_PORT` matches `docker-compose.yml` |
| Duplicate or stale chunks after restart | `RAG_CLEAR_BEFORE_INGEST=false` with no external ingestion managing the table | Set it back to `true`, or manage clearing yourself |
| Empty or irrelevant answers | PDF not found at `RAG_DOCUMENT_PATH`, or relevant content scores below the 0.6 threshold | Verify the PDF path and content; consider adjusting retrieval score threshold |

