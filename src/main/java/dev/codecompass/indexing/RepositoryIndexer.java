package dev.codecompass.indexing;

import dev.codecompass.embedding.NomicEmbeddingText;
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
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;

@Service
public class RepositoryIndexer {
    private static final int EMBEDDING_BATCH_SIZE = 8;
    private static final int INDEX_VERSION = 8;
    private final CodeFileScanner scanner;
    private final CodeChunker chunker;
    private final RepositoryPathResolver pathResolver;
    private final RepositoryIndexStateStore stateStore;
    private final KeywordCodeSearch keywordSearch;
    private final SimpleVectorStore vectorStore;
    private final Path vectorFile;
    private final int maxChunks;
    private final NomicEmbeddingText embeddingText;

    public RepositoryIndexer(CodeFileScanner scanner, CodeChunker chunker, RepositoryPathResolver pathResolver,
                             RepositoryIndexStateStore stateStore, KeywordCodeSearch keywordSearch,
                             SimpleVectorStore vectorStore,
                             @Value("${codecompass.vector-file:./data/ollama-code-vectors.json}") String vectorFile,
                             @Value("${codecompass.indexing.max-chunks:3000}") int maxChunks,
                             NomicEmbeddingText embeddingText) {
        this.scanner = scanner;
        this.chunker = chunker;
        this.pathResolver = pathResolver;
        this.stateStore = stateStore;
        this.keywordSearch = keywordSearch;
        this.vectorStore = vectorStore;
        this.vectorFile = Path.of(vectorFile);
        if (maxChunks < 1) throw new IllegalArgumentException("Index chunk limit must be positive");
        this.maxChunks = maxChunks;
        this.embeddingText = embeddingText;
    }

    public IndexResult index(String requestedPath, boolean reuseExisting) throws IOException {
        Path root = pathResolver.resolve(requestedPath);
        Optional<RepositoryIndexStateStore.Snapshot> previous = stateStore.load();
        if (reuseExisting && Files.exists(vectorFile) && keywordSearch.hasIndex(root.toString())
                && previous.filter(snapshot -> snapshot.repository().equals(root.toString())
                && snapshot.indexVersion() == INDEX_VERSION).isPresent()) {
            var snapshot = previous.orElseThrow();
            return new IndexResult(root.toString(), snapshot.filesIndexed(), snapshot.chunksIndexed(), true);
        }

        List<Document> documents = new ArrayList<>();
        List<Path> paths = scanner.scan(root);
        for (Path path : paths) {
            String relative = root.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
            documents.addAll(chunker.split(Files.readString(path, StandardCharsets.UTF_8), relative));
            if (documents.size() > maxChunks) {
                throw new CodeFileScanner.IndexScanLimitException("Repository produces more than " + maxChunks + " chunks. Add ignored paths to .codecompassignore or raise codecompass.indexing.max-chunks.");
            }
        }
        if (documents.isEmpty()) throw new IllegalStateException("No supported source files were found in " + root);
        List<Document> embeddingDocuments = documents.stream().map(embeddingText::forIndexing).toList();
        if (Files.exists(vectorFile)) vectorStore.delete("path != ''");
        for (int start = 0; start < embeddingDocuments.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, embeddingDocuments.size());
            vectorStore.add(embeddingDocuments.subList(start, end));
        }
        Files.createDirectories(vectorFile.toAbsolutePath().getParent());
        vectorStore.save(vectorFile.toFile());
        keywordSearch.replaceIndex(root.toString(), documents);
        stateStore.save(new RepositoryIndexStateStore.Snapshot(root.toString(), paths.size(), documents.size(), Instant.now().toString(), INDEX_VERSION));
        return new IndexResult(root.toString(), paths.size(), documents.size(), false);
    }

    public record IndexResult(String repository, int files, int chunks, boolean alreadyIndexed) { }
}
