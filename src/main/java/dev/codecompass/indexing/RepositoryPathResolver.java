package dev.codecompass.indexing;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RepositoryPathResolver {
    public Path resolve(String requestedPath) throws IOException {
        String input = requestedPath.trim();
        if (input.equals("~") || input.startsWith("~/")) {
            input = System.getProperty("user.home") + input.substring(1);
        }

        Path root = Path.of(input).toRealPath();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Repository path must point to a directory: " + requestedPath);
        }
        return root;
    }
}
