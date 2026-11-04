package dev.codecompass.indexing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CodeFileScanner {
    private static final Set<String> SUPPORTED = Set.of("java", "kt", "kts", "scala", "groovy", "py", "js", "jsx", "ts", "tsx", "mjs", "cjs", "go", "rs", "c", "h", "cc", "cpp", "hpp", "cs", "fs", "php", "rb", "swift", "m", "mm", "sql", "html", "css", "scss", "vue", "svelte", "xml", "yaml", "yml", "json", "toml", "properties", "prisma", "graphql", "gql", "gradle", "md", "mdx", "sh", "bash", "dockerfile", "tf", "proto");
    private static final long MAX_FILE_BYTES = 1_000_000;
    private final int maxFiles;
    private final int maxEntries;
    private final long maxTotalBytes;

    public CodeFileScanner(@Value("${codecompass.indexing.max-files:5000}") int maxFiles,
                           @Value("${codecompass.indexing.max-entries:100000}") int maxEntries,
                           @Value("${codecompass.indexing.max-total-bytes:104857600}") long maxTotalBytes) {
        if (maxFiles < 1 || maxEntries < 1 || maxTotalBytes < 1) throw new IllegalArgumentException("Index scan limits must be positive");
        this.maxFiles = maxFiles;
        this.maxEntries = maxEntries;
        this.maxTotalBytes = maxTotalBytes;
    }

    public List<Path> scan(Path root) throws IOException {
        RepositoryIgnoreRules ignoreRules = new RepositoryIgnoreRules(root);
        List<Path> files = new ArrayList<>();
        long[] totalBytes = {0};
        int[] visitedEntries = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (!directory.equals(root) && ignoreRules.isIgnored(directory, true)) return FileVisitResult.SKIP_SUBTREE;
                countEntry(visitedEntries);
                ignoreRules.enterDirectory(directory);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                countEntry(visitedEntries);
                if (!attributes.isRegularFile() || ignoreRules.isIgnored(file, false) || !isSupported(file)) return FileVisitResult.CONTINUE;
                long size = attributes.size();
                if (size > MAX_FILE_BYTES) return FileVisitResult.CONTINUE;
                if (!isText(file)) return FileVisitResult.CONTINUE;
                if (files.size() == maxFiles) {
                    throw new IndexScanLimitException("Repository contains more than " + maxFiles + " indexable files. Add ignored paths to .codecompassignore or raise codecompass.indexing.max-files.");
                }
                if (size > maxTotalBytes - totalBytes[0]) {
                    throw new IndexScanLimitException("Indexable source exceeds " + maxTotalBytes + " bytes. Add ignored paths to .codecompassignore or raise codecompass.indexing.max-total-bytes.");
                }
                files.add(file);
                totalBytes[0] += size;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
                throw new IOException("Unable to inspect repository path " + file, exception);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                ignoreRules.leaveDirectory(directory);
                if (exception != null) throw new IOException("Unable to scan repository directory " + directory, exception);
                return FileVisitResult.CONTINUE;
            }
        });
        return List.copyOf(files);
    }

    private void countEntry(int[] visitedEntries) {
        visitedEntries[0]++;
        if (visitedEntries[0] > maxEntries) {
            throw new IndexScanLimitException("Repository traversal exceeds " + maxEntries + " non-ignored paths. Add exclusions to .codecompassignore or raise codecompass.indexing.max-entries.");
        }
    }

    private boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.equals("dockerfile") || name.equals("makefile")) return true;
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED.contains(name.substring(dot + 1));
    }

    private boolean isText(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            byte[] sample = input.readNBytes(8192);
            for (byte value : sample) if (value == 0) return false;
            return true;
        }
    }

    public static class IndexScanLimitException extends IllegalStateException {
        public IndexScanLimitException(String message) { super(message); }
    }
}
