package dev.codecompass.indexing;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class CodeFileScanner {
    private static final Set<String> IGNORED_DIRS = Set.of(".git", ".idea", ".vscode", "node_modules", "vendor", "build", "target", "dist", "out", "coverage", ".next", ".venv", "venv", "__pycache__");
    private static final Set<String> IGNORED_FILES = Set.of("package-lock.json", "yarn.lock", "pnpm-lock.yaml", "bun.lock", "bun.lockb", "composer.lock", "gemfile.lock", "cargo.lock");
    private static final Set<String> SUPPORTED = Set.of("java", "kt", "kts", "scala", "groovy", "py", "js", "jsx", "ts", "tsx", "mjs", "cjs", "go", "rs", "c", "h", "cc", "cpp", "hpp", "cs", "fs", "php", "rb", "swift", "m", "mm", "sql", "html", "css", "scss", "vue", "svelte", "xml", "yaml", "yml", "json", "toml", "properties", "prisma", "graphql", "gql", "gradle", "md", "mdx", "sh", "bash", "dockerfile", "tf", "proto");
    private static final long MAX_FILE_BYTES = 1_000_000;

    public Stream<Path> scan(Path root) throws IOException {
        return Files.walk(root).filter(path -> !isIgnored(root, path)).filter(Files::isRegularFile)
                .filter(this::isSupported).filter(path -> isSmallText(path));
    }

    private boolean isIgnored(Path root, Path path) {
        if (path.equals(root)) return false;
        return root.relativize(path).toString().lines().anyMatch(part -> IGNORED_DIRS.contains(part));
    }

    private boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (IGNORED_FILES.contains(name)) return false;
        if (name.equals("dockerfile") || name.equals("makefile")) return true;
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED.contains(name.substring(dot + 1));
    }

    private boolean isSmallText(Path path) {
        try {
            if (Files.size(path) > MAX_FILE_BYTES) return false;
            byte[] sample = Files.readAllBytes(path);
            int limit = Math.min(sample.length, 8192);
            for (int i = 0; i < limit; i++) if (sample[i] == 0) return false;
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect " + path, exception);
        }
    }
}
