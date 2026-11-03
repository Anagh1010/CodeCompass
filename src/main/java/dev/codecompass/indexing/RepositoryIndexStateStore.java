package dev.codecompass.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class RepositoryIndexStateStore {
    private final ObjectMapper objectMapper;
    private final Path stateFile;

    public RepositoryIndexStateStore(ObjectMapper objectMapper,
                                     @Value("${codecompass.index-state-file:./data/index-state.json}") String stateFile) {
        this.objectMapper = objectMapper;
        this.stateFile = Path.of(stateFile);
    }

    public Optional<Snapshot> load() throws IOException {
        if (!Files.exists(stateFile)) return Optional.empty();
        return Optional.of(objectMapper.readValue(stateFile.toFile(), Snapshot.class));
    }

    public void save(Snapshot snapshot) throws IOException {
        Files.createDirectories(stateFile.toAbsolutePath().getParent());
        objectMapper.writeValue(stateFile.toFile(), snapshot);
    }

    public record Snapshot(String repository, int filesIndexed, int chunksIndexed, String indexedAt, int indexVersion) { }
}
