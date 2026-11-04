package dev.codecompass.qa;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CodeChunkReranker {
    public List<Document> rerank(RagQuery query, List<Document> candidates) {
        return candidates.stream()
                .map(document -> new ScoredDocument(document, score(query, document)))
                .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                .map(this::withScore).toList();
    }

    private double score(RagQuery query, Document document) {
        String body = document.getText().toLowerCase(Locale.ROOT);
        String path = String.valueOf(document.getMetadata().get("path")).toLowerCase(Locale.ROOT);
        double score = metadataScore(document) * 1_000;
        for (String identifier : query.exactIdentifiers()) {
            String normalized = identifier.toLowerCase(Locale.ROOT);
            if (body.contains(normalized)) score += 18;
            if (path.contains(normalized)) score += 24;
        }
        if (!query.terms().isEmpty()) {
            long matched = query.terms().stream().filter(term -> body.contains(term) || path.contains(term)).count();
            score += 7.0 * matched / query.terms().size();
        }
        return score;
    }

    private double metadataScore(Document document) {
        Object value = document.getMetadata().get("ragScore");
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private Document withScore(ScoredDocument scored) {
        Map<String, Object> metadata = new HashMap<>(scored.document().getMetadata());
        metadata.put("rerankScore", scored.score());
        return new Document(scored.document().getText(), metadata);
    }

    private record ScoredDocument(Document document, double score) { }
}
