package dev.codecompass.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeywordIndexStore {
    private final ObjectMapper objectMapper;
    private final Path indexFile;

    public KeywordIndexStore(ObjectMapper objectMapper,
                             @Value("${codecompass.keyword-index-file:./data/keyword-index.json}") String indexFile) {
        this.objectMapper = objectMapper;
        this.indexFile = Path.of(indexFile);
    }

    public void save(String repository, List<Document> documents) throws IOException {
        Files.createDirectories(indexFile.toAbsolutePath().getParent());
        List<StoredChunk> chunks = documents.stream()
                .map(document -> new StoredChunk(document.getText(), document.getMetadata())).toList();
        objectMapper.writeValue(indexFile.toFile(), new StoredIndex(repository, chunks));
    }

    public Optional<List<Document>> load(String repository) throws IOException {
        if (!Files.exists(indexFile)) return Optional.empty();
        StoredIndex stored = objectMapper.readValue(indexFile.toFile(), StoredIndex.class);
        if (!stored.repository().equals(repository)) return Optional.empty();
        return Optional.of(stored.chunks().stream()
                .map(chunk -> new Document(chunk.text(), chunk.metadata())).toList());
    }

    public boolean contains(String repository) throws IOException {
        if (!Files.exists(indexFile)) return false;
        StoredIndex stored = objectMapper.readValue(indexFile.toFile(), StoredIndex.class);
        return stored.repository().equals(repository);
    }

    private record StoredIndex(String repository, List<StoredChunk> chunks) { }
    private record StoredChunk(String text, Map<String, Object> metadata) { }
}
