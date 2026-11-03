# Continuity Ledger

## Snapshot
- 2026-11-03 [USER] Goal: build a Spring Boot universal codebase Q&A assistant.
- 2026-11-03 [USER] Runtime choice: Ollama with `qwen2.5-coder:3b` (4096 context) and `nomic-embed-text`, for M2/8 GB.
- 2026-11-03 [CODE] App has repository indexing, persistent local vector store, reuse toggle, REST Q&A, and static UI.
- 2026-11-03 [CODE] Q&A now combines keyword and vector retrieval; keyword chunks are cached after loading/indexing.
- 2026-11-03 [CODE] Chunks prefer class/function declaration boundaries; index version 4 requires a refresh.
- 2026-11-03 [CODE] Answer API/UI return file-and-line references only; source excerpts are not returned or displayed.
- 2026-11-03 [USER] Provider choice: offer local Ollama or Gemini for Q&A.
- 2026-11-03 [CODE] Added Q&A provider selector; Ollama remains the default, Gemini 3.8 Flash uses server-side key from `.secrets` or `GEMINI_API_KEY`.
- 2026-11-03 [CODE] Gemini UI discloses that retrieved code excerpts are sent to Google; vector embeddings/indexing remain local.
- 2026-11-03 [CODE] Both providers now receive detailed-answer instructions with 900-token Ollama and 1200-token Gemini output limits.
- 2026-11-03 [USER] User requests a Vite frontend with larger typography and less unused horizontal space.
- 2026-11-03 [CODE] Replaced static starter UI sources with responsive React/Vite workspace; Vite proxies API in dev and builds into Spring static resources.
- 2026-11-03 [TOOL] Vite production build and Maven package succeeded with generated frontend assets.
- 2026-11-03 [USER] User requests stopping services and closing the project.
- 2026-11-03 [TOOL] Stopped Maven Java server and Homebrew Ollama service; ports 8080, 5173, and 11434 have no listeners.
- 2026-11-03 [TOOL] Maven package succeeded after retrieval, UI, and Gemini provider changes.
- 2026-11-03 [ASSUMPTION] Roamwise requires restart/re-index against index version 4 before updated retrieval is active.
- 2026-11-03 [ASSUMPTION] Open question: persistent production vector database and multi-repository management are future decisions.

## Decisions
- 2026-11-03 [USER] D005 ACTIVE: local and Gemini Q&A responses should be detailed while remaining grounded in indexed evidence.
- 2026-11-03 [USER] D001 ACTIVE: use local Ollama; Qwen2.5-Coder 3B at 4096 context plus Nomic embeddings.
- 2026-11-03 [USER] D004 ACTIVE: Q&A can select local Ollama or optional Gemini Developer API; indexing/embeddings remain local Ollama.
- 2026-11-03 [CODE] D002 ACTIVE: scan a user-supplied local directory, skipping generated/dependency paths and common lockfiles.
- 2026-11-03 [CODE] D003 ACTIVE: persist vector data and index metadata under `data/`; allow explicit reuse of a compatible saved index.

## Done (recent)
- 2026-11-03 [CODE] Implemented Spring Boot API, repository scanner/chunker, vector retrieval, grounded answers, and browser UI.
- 2026-11-03 [CODE] Added `reuseExisting` true/false toggle; index format now version 4 after chunking changes.
- 2026-11-03 [CODE] Added mixed keyword/vector retrieval, declaration-aware chunk cuts, and citation-only source display.
- 2026-11-03 [CODE] Added Gemini Q&A provider selection and server-side API key loading.
- 2026-11-03 [CODE] Increased answer detail instructions and output limits for both providers.
- 2026-11-03 [CODE] Added responsive Vite/React UI with full-width Q&A panel, compact repository rail, larger type, markdown rendering, and mobile layout.
- 2026-11-03 [TOOL] `npm install`, `npm run build`, and `mvn -q -DskipTests package` succeeded; tests were not run.

## Working set
- 2026-11-03 [CODE] `src/main/java/dev/codecompass/qa/CodebaseAssistant.java`
- 2026-11-03 [CODE] `src/main/java/dev/codecompass/qa/GeminiAnswerClient.java`
- 2026-11-03 [CODE] `src/main/java/dev/codecompass/indexing/KeywordCodeSearch.java`
- 2026-11-03 [CODE] `src/main/java/dev/codecompass/indexing/`
- 2026-11-03 [CODE] `src/main/resources/static/`
- 2026-11-03 [CODE] `frontend/`
- 2026-11-03 [CODE] `src/main/resources/application.yml`
- 2026-11-03 [CODE] `data/index-state.json`
- 2026-11-03 [CODE] `README.md`
- 2026-11-03 [CODE] `CONTINUITY.md`

## Receipts
- 2026-11-03 [TOOL] Runtime UI/API previously verified at localhost:8080; current server process is UNCONFIRMED.
- 2026-11-03 [TOOL] Last persisted Roamwise index was version 3; version 4 will trigger refresh.
- 2026-11-03 [TOOL] Maven package succeeded after latest changes; tests were not run.
