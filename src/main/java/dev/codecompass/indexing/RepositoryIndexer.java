package dev.codecompass.indexing;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;

@Service
public class RepositoryIndexer {
    private static final int EMBEDDING_BATCH_SIZE = 8;
    private static final int INDEX_VERSION = 4;
    private final CodeFileScanner scanner;
    private final CodeChunker chunker;
    private final RepositoryPathResolver pathResolver;
    private final RepositoryIndexStateStore stateStore;
    private final KeywordCodeSearch keywordSearch;
    private final SimpleVectorStore vectorStore;
    private final Path vectorFile;

    public RepositoryIndexer(CodeFileScanner scanner, CodeChunker chunker, RepositoryPathResolver pathResolver,
                             RepositoryIndexStateStore stateStore, KeywordCodeSearch keywordSearch,
                             SimpleVectorStore vectorStore,
                             @Value("${codecompass.vector-file:./data/ollama-code-vectors.json}") String vectorFile) {
        this.scanner = scanner;
        this.chunker = chunker;
        this.pathResolver = pathResolver;
        this.stateStore = stateStore;
        this.keywordSearch = keywordSearch;
        this.vectorStore = vectorStore;
        this.vectorFile = Path.of(vectorFile);
    }

    public IndexResult index(String requestedPath, boolean reuseExisting) throws IOException {
        Path root = pathResolver.resolve(requestedPath);
        Optional<RepositoryIndexStateStore.Snapshot> previous = stateStore.load();
        if (reuseExisting && Files.exists(vectorFile) && previous.filter(snapshot ->
                snapshot.repository().equals(root.toString()) && snapshot.indexVersion() == INDEX_VERSION).isPresent()) {
            var snapshot = previous.orElseThrow();
            return new IndexResult(root.toString(), snapshot.filesIndexed(), snapshot.chunksIndexed(), true);
        }

        List<Document> documents = new ArrayList<>();
        AtomicInteger files = new AtomicInteger();
        try (var paths = scanner.scan(root)) {
            paths.forEach(path -> {
                try {
                    String relative = root.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
                    documents.addAll(chunker.split(Files.readString(path, StandardCharsets.UTF_8), relative));
                    files.incrementAndGet();
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to read " + path, exception);
                }
            });
        }
        if (documents.isEmpty()) throw new IllegalStateException("No supported source files were found in " + root);
        keywordSearch.replaceIndex(root.toString(), documents);

        if (Files.exists(vectorFile)) vectorStore.delete("path != ''");
        for (int start = 0; start < documents.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, documents.size());
            vectorStore.add(documents.subList(start, end));
        }
        Files.createDirectories(vectorFile.toAbsolutePath().getParent());
        vectorStore.save(vectorFile.toFile());
        stateStore.save(new RepositoryIndexStateStore.Snapshot(root.toString(), files.get(), documents.size(), Instant.now().toString(), INDEX_VERSION));
        return new IndexResult(root.toString(), files.get(), documents.size(), false);
    }

    public record IndexResult(String repository, int files, int chunks, boolean alreadyIndexed) { }
}
