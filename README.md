# CodeCompass

CodeCompass indexes repositories locally with Ollama embeddings and answers questions using either local Ollama or the Gemini Developer API. Its retrieval-augmented generation (RAG) flow analyzes the question, searches by BM25 keywords and semantic similarity, fuses and reranks the candidates, assembles a bounded source context, then generates a cited answer. The API returns source paths and line ranges, not code excerpts.

## Requirements

- Java 21 or newer
- Maven 3.9+
- Node.js 20.19+ or 22.12+
- [Ollama](https://ollama.com/) running locally
- A Gemini API key in `.secrets` or `GEMINI_API_KEY` if you want Gemini answers

Pull the default chat and embedding models:

```bash
ollama pull qwen2.5-coder:3b
ollama pull nomic-embed-text
```

The first model answers locally; the second creates embeddings. Indexing uses Ollama in either Q&A mode. Nomic receives `search_document:`-prefixed chunks with file path and language context, and `search_query:`-prefixed semantic queries. Select **Local Qwen 3B** or **Gemini 3.8 Flash** in the chat panel. Gemini receives the retrieved source excerpts for the selected question; the API key stays on the server. Indexing sends batches of eight chunks to Ollama and chunks source near declarations where possible.

## Development

Run Spring Boot in one terminal:

```bash
mvn spring-boot:run
```

Run the Vite frontend in another terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` and `/actuator` requests to Spring Boot at port 8080. The responsive workspace gives repository setup a compact side rail and uses the remaining viewport for the Q&A conversation.

## Production frontend

Build the frontend into Spring Boot's static resources, then package and run the backend:

```bash
npm --prefix frontend ci
npm --prefix frontend run build
mvn package
mvn spring-boot:run
```

Open http://localhost:8080. The generated Vite assets are served by Spring Boot. The vector index is saved under `./data/ollama-code-vectors.json`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_CHAT_MODEL` | `qwen2.5-coder:3b` | Chat model |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | Embedding model |
| `CODECOMPASS_VECTOR_FILE` | `./data/ollama-code-vectors.json` | Persistent vector index location |
| `CODECOMPASS_KEYWORD_INDEX_FILE` | `./data/keyword-index.json` | Persistent BM25 corpus for hybrid retrieval |
| `CODECOMPASS_INDEX_STATE_FILE` | `./data/index-state.json` | Repository and index metadata |
| `GEMINI_API_KEY` | value from `.secrets` | Optional Gemini Developer API key |
| `GEMINI_CHAT_MODEL` | `gemini-3.8-flash` | Gemini Q&A model |
| `PORT` | `8080` | HTTP port |

The REST API is `POST /api/index` with `{ "path": "~/projects/repository", "reuseExisting": true }` (absolute and `~/` paths are supported), and `POST /api/ask` with `{ "question": "Where is routing configured?", "provider": "ollama" }`. Set `provider` to `gemini` to use Gemini. Actuator health is available at `/actuator/health`.

To use the ignored `.secrets` properties file, add `gemini_api_key=YOUR_KEY` to it. Alternatively, set `GEMINI_API_KEY` in the environment. Gemini is optional; local Q&A does not need this key.

The scanner indexes common programming, configuration, and documentation formats. It prunes common dependency, build, cache, generated, and data directories before traversing them; honors nested `.gitignore` and `.codecompassignore` rules; skips common lockfiles, generated bundles/maps, credential files, and source files over 1 MB. Each repository is bounded to 100,000 traversed paths, 5,000 source files, 100 MB of source, and 3,000 chunks by default. Configure these ceilings with `CODECOMPASS_INDEX_MAX_ENTRIES`, `CODECOMPASS_INDEX_MAX_FILES`, `CODECOMPASS_INDEX_MAX_TOTAL_BYTES`, and `CODECOMPASS_INDEX_MAX_CHUNKS`. A limit error happens before the saved index is replaced. Put additional exclusions in a `.codecompassignore` file at the repository root using standard Git ignore syntax. Retrieved chunks are ranked with reciprocal-rank fusion and code identifier matches, then limited to relevant excerpts from up to two sections per file. The vector store is local and single-process; use a persistent vector database when moving to shared or large deployments.

The **Use existing index** toggle is on by default: a matching vector and keyword index pair is reused; turn it off to rebuild both indexes. The vector data, BM25 corpus, and repository metadata are stored locally under `./data/`.
