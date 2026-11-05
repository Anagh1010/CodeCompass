# Continuity Ledger

## Snapshot
- 2026-11-03 [USER] Goal: build a Spring Boot universal codebase Q&A assistant.
- 2026-11-03 [USER] Runtime choice: Ollama with `qwen2.5-coder:3b` (4096 context) and `nomic-embed-text`, for M2/8 GB.
- 2026-11-03 [CODE] App has repository indexing, persistent local vector store, reuse toggle, REST Q&A, and static UI.
- 2026-11-03 [CODE] Q&A now combines keyword and vector retrieval; keyword chunks are cached after loading/indexing.
- 2026-11-05 [CODE] Nomic embeddings use document/query task prefixes plus file/language context; indexed vector format version 8 requires refresh.
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
- 2026-11-03 [USER] User requests a proper RAG pipeline.
- 2026-11-05 [CODE] RAG checks topic overlap, preserves technical identifiers such as `GPS`, and adds references to relevant retrieved chunks when the model omits inline citations.
- 2026-11-04 [CODE] BM25 chunks are persisted alongside vectors in `data/keyword-index.json`; reuse requires matching repository and index version 6.
- 2026-11-03 [TOOL] Vite build and Maven package succeeded after RAG pipeline changes; no tests were run.
- 2026-11-04 [USER] User reports self-indexing is taking a long time.
- 2026-11-04 [TOOL] Found ignored-directory filtering used String.lines(), so `node_modules` and `target` were not excluded; interrupted run had created 10,073 chunks.
- 2026-11-04 [CODE] Scanner now iterates path segments and excludes `data`; keyword sidecar persistence happens after vector save; index version 6 forces rebuild.
- 2026-11-04 [TOOL] Stopped the stuck Spring Boot process; no Maven/Spring process or listeners remain on ports 8080/5173. Maven package succeeded after scanner fix.
- 2026-11-04 [USER] User asks to resume the interrupted self-indexing run.
- 2026-11-04 [TOOL] Re-indexed CodeCompass successfully: 40 files, 82 chunks, index version 6, roughly 13 seconds; vector and BM25 counts agree.
- 2026-11-04 [TOOL] Latest check: no listener on port 8080; Ollama Homebrew service reports `none`.
- 2026-11-04 [TOOL] Maven package succeeded after scanner exclusions and hard ceilings; live re-index was not run because Ollama is stopped.
- 2026-11-03 [ASSUMPTION] Open question: persistent production vector database and multi-repository management are future decisions.

## Decisions
- 2026-11-03 [USER] D005 ACTIVE: local and Gemini Q&A responses should be detailed while remaining grounded in indexed evidence.
- 2026-11-03 [USER] D001 ACTIVE: use local Ollama; Qwen2.5-Coder 3B at 4096 context plus Nomic embeddings.
- 2026-11-03 [USER] D004 ACTIVE: Q&A can select local Ollama or optional Gemini Developer API; indexing/embeddings remain local Ollama.
- 2026-11-03 [CODE] D002 ACTIVE: scan a user-supplied local directory, skipping generated/dependency paths and common lockfiles.
- 2026-11-03 [CODE] D003 ACTIVE: persist vector data and index metadata under `data/`; allow explicit reuse of a compatible saved index.

## Done (recent)
- 2026-11-03 [CODE] Implemented Spring Boot API, repository scanner/chunker, vector retrieval, grounded answers, and browser UI.
- 2026-11-04 [CODE] Added `reuseExisting` true/false toggle; index format was version 6 before scanner hardening.
- 2026-11-05 [CODE] Added Git-compatible repository ignore rules and bounded scans/indexes; index format now version 7 before embedding-prefix change.
- 2026-11-04 [CODE] Added BM25, semantic/symbol channels, reciprocal-rank fusion, reranking, bounded context, line citations, and evidence/citation guards.
- 2026-11-03 [CODE] Added Gemini Q&A provider selection and server-side API key loading.
- 2026-11-03 [CODE] Increased answer detail instructions and output limits for both providers.
- 2026-11-03 [CODE] Added responsive Vite/React UI with full-width Q&A panel, compact repository rail, larger type, markdown rendering, and mobile layout.

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
- 2026-11-04 [TOOL] Prior persisted CodeCompass index: 40 files / 82 chunks, index version 6; version 8 will trigger re-index.
- 2026-11-04 [TOOL] Maven package succeeded after scanner changes; no tests or live re-index run.
- 2026-11-04 [TOOL] Maven/Spring Boot and Vite are stopped; no port 8080/5173 listener. Ollama Homebrew service is stopped.
- 2026-11-05 [TOOL] Maven package succeeded after adding Nomic task prefixes and embedding-only file/language context; no tests or live re-index run.
- 2026-11-04 [TOOL] Maven package succeeded after RAG evidence/citation guard changes; no tests or live model queries were run.
- 2026-11-04 [TOOL] Maven package succeeded after preserving technical identifiers and auto-citing relevant chunks when model output omits markers; no live query run.
