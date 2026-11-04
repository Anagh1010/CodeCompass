package dev.codecompass.indexing;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeywordCodeSearch {
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "from", "does", "where", "what", "how", "this", "that", "are", "was", "were", "into", "being", "used", "using", "use", "which", "main", "application", "project", "codebase", "file");
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final RepositoryIndexStateStore stateStore;
    private final KeywordIndexStore indexStore;
    private volatile KeywordIndex cachedIndex;

    public KeywordCodeSearch(RepositoryIndexStateStore stateStore, KeywordIndexStore indexStore) {
        this.stateStore = stateStore;
        this.indexStore = indexStore;
    }

    public List<Document> search(String question) {
        List<String> queryTerms = new ArrayList<>(new LinkedHashSet<>(terms(question)));
        if (queryTerms.isEmpty()) return List.of();
        try {
            var snapshot = stateStore.load();
            if (snapshot.isEmpty()) return List.of();
            KeywordIndex index = indexFor(snapshot.orElseThrow().repository());
            return index.documents().stream()
                    .map(document -> new ScoredDocument(document.document(), bm25(document, queryTerms, index)))
                    .filter(result -> result.score() > 0)
                    .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                    .limit(20).map(ScoredDocument::document).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to search indexed source files", exception);
        }
    }

    public void replaceIndex(String repository, List<Document> documents) throws IOException {
        synchronized (this) {
            indexStore.save(repository, documents);
            cachedIndex = buildIndex(repository, documents);
        }
    }

    public boolean hasIndex(String repository) throws IOException {
        return indexStore.contains(repository);
    }

    private KeywordIndex indexFor(String repository) throws IOException {
        KeywordIndex current = cachedIndex;
        if (current != null && current.repository().equals(repository)) return current;
        synchronized (this) {
            current = cachedIndex;
            if (current != null && current.repository().equals(repository)) return current;
            List<Document> documents = indexStore.load(repository).orElseThrow(() ->
                    new IllegalStateException("The keyword index is missing. Re-index this repository before asking questions."));
            cachedIndex = buildIndex(repository, documents);
            return cachedIndex;
        }
    }

    private KeywordIndex buildIndex(String repository, List<Document> documents) {
        List<TokenDocument> tokenDocuments = new ArrayList<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        int totalLength = 0;
        for (Document document : documents) {
            List<String> tokens = terms(document.getText());
            Map<String, Integer> frequencies = frequencies(tokens);
            for (String term : frequencies.keySet()) documentFrequency.merge(term, 1, Integer::sum);
            tokenDocuments.add(new TokenDocument(document, frequencies, tokens.size()));
            totalLength += tokens.size();
        }
        double averageLength = tokenDocuments.isEmpty() ? 1 : (double) totalLength / tokenDocuments.size();
        return new KeywordIndex(repository, tokenDocuments, documentFrequency, averageLength);
    }

    private double bm25(TokenDocument candidate, List<String> queryTerms, KeywordIndex index) {
        double score = 0;
        String path = String.valueOf(candidate.document().getMetadata().get("path")).toLowerCase(Locale.ROOT);
        for (String term : queryTerms) {
            int frequency = candidate.frequencies().getOrDefault(term, 0);
            if (frequency == 0) continue;
            int documentFrequency = index.documentFrequency().getOrDefault(term, 0);
            double inverseFrequency = Math.log(1 + (index.documents().size() - documentFrequency + 0.5) / (documentFrequency + 0.5));
            double lengthNorm = K1 * (1 - B + B * candidate.length() / index.averageLength());
            score += inverseFrequency * (frequency * (K1 + 1)) / (frequency + lengthNorm);
            if (path.contains(term)) score += inverseFrequency * 0.25;
        }
        return score;
    }

    private Map<String, Integer> frequencies(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) frequencies.merge(token, 1, Integer::sum);
        return frequencies;
    }

    private List<String> terms(String text) {
        List<String> terms = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            String expanded = token.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
            for (String part : expanded.split("\\s+")) {
                String normalized = part.toLowerCase(Locale.ROOT);
                if (normalized.length() >= 2 && !STOP_WORDS.contains(normalized)) terms.add(normalized);
            }
        }
        return terms;
    }

    private record TokenDocument(Document document, Map<String, Integer> frequencies, int length) { }
    private record KeywordIndex(String repository, List<TokenDocument> documents,
                                Map<String, Integer> documentFrequency, double averageLength) { }
    private record ScoredDocument(Document document, double score) { }
}
