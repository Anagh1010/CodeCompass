package dev.codecompass.indexing;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeywordCodeSearch {
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "from", "does", "where", "what", "how", "this", "that", "are", "was", "were", "into", "being", "used", "using", "use", "which", "main", "application", "project", "codebase", "file");

    private final CodeFileScanner scanner;
    private final CodeChunker chunker;
    private final RepositoryIndexStateStore stateStore;
    private volatile String cachedRepository;
    private volatile List<Document> cachedDocuments = List.of();

    public KeywordCodeSearch(CodeFileScanner scanner, CodeChunker chunker, RepositoryIndexStateStore stateStore) {
        this.scanner = scanner;
        this.chunker = chunker;
        this.stateStore = stateStore;
    }

    public List<Document> search(String question) {
        List<String> terms = terms(question);
        if (terms.isEmpty()) return List.of();
        try {
            var snapshot = stateStore.load();
            if (snapshot.isEmpty()) return List.of();
            String repository = snapshot.orElseThrow().repository();
            List<Document> documents = documentsFor(repository);
            List<ScoredDocument> matches = new ArrayList<>();
            for (Document document : documents) {
                int score = score(document, terms);
                if (score >= 4) matches.add(new ScoredDocument(document, score));
            }
            return matches.stream().sorted(Comparator.comparingInt(ScoredDocument::score).reversed())
                    .limit(8).map(ScoredDocument::document).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to search indexed source files", exception);
        }
    }

    public void replaceIndex(String repository, List<Document> documents) {
        synchronized (this) {
            cachedDocuments = List.copyOf(documents);
            cachedRepository = repository;
        }
    }

    private List<Document> documentsFor(String repository) throws IOException {
        if (repository.equals(cachedRepository)) return cachedDocuments;
        synchronized (this) {
            if (repository.equals(cachedRepository)) return cachedDocuments;
            Path root = Path.of(repository);
            List<Document> documents = new ArrayList<>();
            try (var paths = scanner.scan(root)) {
                for (Path file : paths.toList()) {
                    String relative = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
                    documents.addAll(chunker.split(Files.readString(file, StandardCharsets.UTF_8), relative));
                }
            }
            cachedDocuments = List.copyOf(documents);
            cachedRepository = repository;
            return cachedDocuments;
        }
    }

    private int score(Document document, List<String> terms) {
        String body = document.getText().toLowerCase(Locale.ROOT);
        String path = String.valueOf(document.getMetadata().get("path")).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (body.contains(term)) score += 2;
            if (path.contains(term)) score += 5;
        }
        return score;
    }

    private List<String> terms(String question) {
        List<String> terms = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(question);
        while (matcher.find()) {
            String token = matcher.group();
            String[] parts = token.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT).split("\\s+");
            for (String part : parts) if (part.length() >= 3 && !STOP_WORDS.contains(part) && !terms.contains(part)) terms.add(part);
        }
        return terms;
    }

    private record ScoredDocument(Document document, int score) { }
}
