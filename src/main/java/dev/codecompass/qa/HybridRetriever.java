package dev.codecompass.qa;

import dev.codecompass.embedding.NomicEmbeddingText;
import dev.codecompass.indexing.KeywordCodeSearch;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HybridRetriever {
    private static final int SEMANTIC_CANDIDATES = 18;
    private static final int SYMBOL_CANDIDATES = 12;
    private static final String STARTUP_FILE_FILTER = "path in ['package.json', 'pom.xml', 'build.gradle', 'build.gradle.kts', 'pyproject.toml', 'Cargo.toml', 'go.mod', 'Makefile', 'Dockerfile']";

    private final VectorStore vectorStore;
    private final KeywordCodeSearch keywordSearch;
    private final NomicEmbeddingText embeddingText;

    public HybridRetriever(VectorStore vectorStore, KeywordCodeSearch keywordSearch, NomicEmbeddingText embeddingText) {
        this.vectorStore = vectorStore;
        this.keywordSearch = keywordSearch;
        this.embeddingText = embeddingText;
    }

    public RankedCandidates retrieve(RagQuery query) {
        List<RankedCandidates.RankedList> channels = new ArrayList<>();
        channels.add(new RankedCandidates.RankedList("bm25", 1.2, keywordSearch.search(query.original())));
        channels.add(new RankedCandidates.RankedList("semantic", 1.0, vectorSearch(query.semanticQuery(), SEMANTIC_CANDIDATES, 0.25)));

        if (!query.exactIdentifiers().isEmpty()) {
            String symbols = String.join(" ", query.exactIdentifiers());
            channels.add(new RankedCandidates.RankedList("symbols", 1.25, vectorSearch(symbols, SYMBOL_CANDIDATES, 0.2)));
        }
        if (query.startupIntent()) {
            List<Document> startupFiles = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(embeddingText.forQuery("application startup command run script main entry point"))
                    .topK(8).filterExpression(STARTUP_FILE_FILTER).build());
            channels.add(new RankedCandidates.RankedList("startup", 1.35, startupFiles));
        }
        return new RankedCandidates(List.copyOf(channels));
    }

    private List<Document> vectorSearch(String query, int topK, double threshold) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(embeddingText.forQuery(query)).topK(topK).similarityThreshold(threshold).build());
    }
}
