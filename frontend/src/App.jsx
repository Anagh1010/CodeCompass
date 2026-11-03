import { useEffect, useRef, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {
  ArrowDown, ArrowRight, ArrowUp, Check, ChevronDown, CircleHelp, Code2,
  FolderGit2, LoaderCircle, MessageSquareText, Search, ShieldCheck, Sparkles,
} from "lucide-react";

const suggestions = [
  { icon: Code2, label: "Trace a request", question: "Trace how a request moves through this project." },
  { icon: FolderGit2, label: "Find the entry point", question: "Where does the application start?" },
  { icon: CircleHelp, label: "Explain a key concept", question: "What are the main modules and how do they fit together?" },
];

async function readResponse(response) {
  const payload = await response.json();
  if (!response.ok) throw new Error(payload.detail || payload.message || payload.error || "The request failed.");
  return payload;
}

export default function App() {
  const [repositoryPath, setRepositoryPath] = useState("");
  const [repository, setRepository] = useState("");
  const [reuseExisting, setReuseExisting] = useState(true);
  const [provider, setProvider] = useState("ollama");
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [indexing, setIndexing] = useState(false);
  const [asking, setAsking] = useState(false);
  const [indexStatus, setIndexStatus] = useState(null);
  const [indexError, setIndexError] = useState(false);
  const conversationRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    conversationRef.current?.scrollTo({ top: conversationRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, asking]);

  async function indexRepository(event) {
    event.preventDefault();
    setIndexing(true);
    setIndexStatus(reuseExisting ? "Checking for an existing index…" : "Reading files and building the index…");
    setIndexError(false);
    try {
      const response = await fetch("/api/index", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ path: repositoryPath, reuseExisting }),
      });
      const result = await readResponse(response);
      setRepository(result.repository);
      setRepositoryPath(result.repository);
      setIndexStatus(result.alreadyIndexed
        ? `Using saved index · ${result.filesIndexed} files · ${result.chunksIndexed} sections`
        : `Indexed ${result.filesIndexed} files · ${result.chunksIndexed} sections`);
    } catch (error) {
      setIndexStatus(error.message);
      setIndexError(true);
    } finally {
      setIndexing(false);
    }
  }

  async function askCodebase(value = question) {
    const prompt = value.trim();
    if (!prompt || asking) return;
    setQuestion("");
    setMessages((current) => [...current, { role: "user", content: prompt }, { role: "assistant", pending: true }]);
    setAsking(true);
    try {
      const response = await fetch("/api/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: prompt, provider }),
      });
      const result = await readResponse(response);
      setMessages((current) => current.map((message, index) => index === current.length - 1
        ? { role: "assistant", content: result.answer, sources: result.sources || [] }
        : message));
    } catch (error) {
      setMessages((current) => current.map((message, index) => index === current.length - 1
        ? { role: "assistant", content: error.message, error: true }
        : message));
    } finally {
      setAsking(false);
      inputRef.current?.focus();
    }
  }

  function submitQuestion(event) {
    event.preventDefault();
    askCodebase();
  }

  return (
    <div className="app-frame">
      <aside className="side-rail">
        <a className="brand" href="#home" aria-label="CodeCompass home">
          <span className="brand-icon"><Search size={19} strokeWidth={2.3} /></span>
          <span>codecompass</span>
        </a>

        <div className="rail-heading">WORKSPACE</div>
        <section className="repo-card">
          <div className="section-title"><FolderGit2 size={17} /><span>Repository</span></div>
          <form onSubmit={indexRepository} className="repo-form">
            <label htmlFor="repo-path">Local project folder</label>
            <input
              id="repo-path"
              value={repositoryPath}
              onChange={(event) => setRepositoryPath(event.target.value)}
              placeholder="~/projects/my-project"
              required
            />
            <label className="reuse-control">
              <input type="checkbox" checked={reuseExisting} onChange={(event) => setReuseExisting(event.target.checked)} />
              <span className="custom-check"><Check size={12} /></span>
              <span>Use existing index</span>
            </label>
            <button className="index-button" type="submit" disabled={indexing}>
              {indexing ? <LoaderCircle className="spin" size={16} /> : <ArrowRight size={16} />}
              {indexing ? "Indexing" : "Index repository"}
            </button>
          </form>
          {indexStatus && <div className={`index-status ${indexError ? "is-error" : ""}`} role="status">{indexStatus}</div>}
          {repository && <div className="connected-repo"><span className="status-dot" />{repository}</div>}
        </section>

        <div className="rail-spacer" />
        <div className="privacy-card">
          <ShieldCheck size={17} />
          <div><strong>Private by default</strong><p>Code is indexed on this machine. Gemini sends retrieved excerpts to Google when selected.</p></div>
        </div>
        <div className="rail-footer"><span className="online-dot" />LOCAL WORKSPACE <span className="version">0.1</span></div>
      </aside>

      <main className="main-panel" id="home">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><ChevronDown size={14} /><strong>Codebase Q&amp;A</strong></div>
          <label className="model-picker">
            <span>MODEL</span>
            <select value={provider} onChange={(event) => setProvider(event.target.value)} aria-label="Answer model">
              <option value="ollama">Local Qwen 3B</option>
              <option value="gemini">Gemini 3.8 Flash</option>
            </select>
            <ChevronDown className="picker-chevron" size={13} />
          </label>
        </header>

        <section className="conversation-area" ref={conversationRef}>
          {messages.length === 0 ? (
            <div className="welcome-state">
              <div className="welcome-icon"><Sparkles size={21} /></div>
              <div className="welcome-kicker">YOUR CODEBASE, UNDERSTOOD</div>
              <h1>What are you curious about?</h1>
              <p className="welcome-copy">Ask a question and get a detailed answer grounded in your project’s source code.</p>
              <div className="suggestion-grid">
                {suggestions.map(({ icon: Icon, label, question: suggestedQuestion }) => (
                  <button key={label} className="suggestion-card" onClick={() => askCodebase(suggestedQuestion)}>
                    <span className="suggestion-icon"><Icon size={17} /></span>
                    <span>{label}</span>
                    <ArrowRight className="suggestion-arrow" size={15} />
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="message-list">
              {messages.map((message, index) => (
                <article key={`${message.role}-${index}`} className={`message-row ${message.role}`}>
                  <div className={`avatar ${message.role}`}>{message.role === "assistant" ? <span>◈</span> : "Y"}</div>
                  <div className="message-content">
                    <div className="message-heading">{message.role === "assistant" ? "CodeCompass" : "You"}</div>
                    {message.pending ? (
                      <div className="thinking"><span /><span /><span />Searching relevant code…</div>
                    ) : (
                      <div className={`answer-text ${message.error ? "answer-error" : ""}`}>
                        <ReactMarkdown remarkPlugins={[remarkGfm]}>{message.content}</ReactMarkdown>
                      </div>
                    )}
                    {message.sources?.length > 0 && (
                      <div className="source-list" aria-label="Sources">
                        {message.sources.map((source, sourceIndex) => (
                          <span key={`${source.path}-${source.line}`} className="source-pill">
                            <Code2 size={13} />{source.path}:{source.line}<span className="source-number">[{sourceIndex + 1}]</span>
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <div className="composer-wrap">
          <form className="composer" onSubmit={submitQuestion}>
            <textarea
              ref={inputRef}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  submitQuestion(event);
                }
              }}
              placeholder="Ask about your codebase…"
              rows={1}
              aria-label="Your question"
            />
            <button type="submit" disabled={!question.trim() || asking} aria-label="Send question">
              {asking ? <LoaderCircle className="spin" size={18} /> : <ArrowUp size={19} />}
            </button>
          </form>
          <div className="composer-meta">
            <span><MessageSquareText size={13} />{provider === "gemini" ? "Gemini · retrieved code is sent to Google" : "Local model · code stays on this machine"}</span>
            <span><ArrowDown size={12} />Enter to send · Shift + Enter for a new line</span>
          </div>
        </div>
      </main>
    </div>
  );
}
