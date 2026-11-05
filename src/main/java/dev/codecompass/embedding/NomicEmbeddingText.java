package dev.codecompass.embedding;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Builds the task-prefixed text sent to Nomic without changing source text shown to the answer model. */
@Component
public class NomicEmbeddingText {
    private static final String DOCUMENT_PREFIX = "search_document: ";
    private static final String QUERY_PREFIX = "search_query: ";
    private static final String CODE_MARKER = "\nCode:\n";

    public Document forIndexing(Document source) {
        String path = String.valueOf(source.getMetadata().get("path"));
        String embeddingText = DOCUMENT_PREFIX + "File: " + path + "\nLanguage: " + language(path)
                + CODE_MARKER + source.getText();
        return new Document(embeddingText, source.getMetadata());
    }

    public String forQuery(String query) {
        return QUERY_PREFIX + query;
    }

    public String sourceText(String storedText) {
        if (!storedText.startsWith(DOCUMENT_PREFIX)) return storedText;
        int marker = storedText.indexOf(CODE_MARKER);
        if (marker < 0) return storedText.substring(DOCUMENT_PREFIX.length());
        return storedText.substring(marker + CODE_MARKER.length());
    }

    private String language(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) return "unknown";
        return path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
