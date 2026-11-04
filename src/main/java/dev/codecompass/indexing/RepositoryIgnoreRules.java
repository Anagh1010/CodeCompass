package dev.codecompass.indexing;

import org.eclipse.jgit.ignore.IgnoreNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Git-compatible ignore rules plus CodeCompass safety exclusions. */
final class RepositoryIgnoreRules {
    private static final String[] BUILTIN_PATTERNS = {
            "# dependency and generated metadata", "package-lock.json", "npm-shrinkwrap.json", "yarn.lock",
            "pnpm-lock.yaml", "bun.lock", "bun.lockb", "composer.lock", "Gemfile.lock", "Cargo.lock",
            "Pipfile.lock", "poetry.lock", "uv.lock", "pdm.lock", "go.sum", "gradle.lockfile",
            "Podfile.lock", "Package.resolved", "flake.lock", "*.min.js", "*.min.css", "*.map",
            "*.bundle.js", "*.chunk.js", "*.generated.*", "*.g.*", "*.designer.*",
            "# credentials and private keys", ".env", ".env.*", "!.env.example", "!.env.sample",
            "!.env.template", ".secrets", ".secrets.*", "*.pem", "*.key", "*.p12", "*.pfx",
            "*.jks", "*.keystore", "id_rsa", "id_rsa.*", "id_ed25519", "id_ed25519.*",
            "credentials*.json", "service-account*.json", "application-secrets.*", "*-secrets.*"
    };

    private final Path root;
    private final IgnoreNode builtins = parse(String.join("\n", BUILTIN_PATTERNS));
    private final List<RuleFile> ruleFiles = new ArrayList<>();

    RepositoryIgnoreRules(Path root) {
        this.root = root;
    }

    boolean isIgnored(Path path, boolean directory) throws IOException {
        if (path.equals(root)) return false;
        if (directory && DEFAULT_IGNORED_DIRECTORIES.contains(path.getFileName().toString().toLowerCase(Locale.ROOT))) return true;
        String relative = unix(root.relativize(path));
        boolean ignored = applies(builtins, relative, directory, false);
        for (RuleFile ruleFile : ruleFiles) {
            if (!path.startsWith(ruleFile.base())) continue;
            String local = unix(ruleFile.base().relativize(path));
            Boolean result = match(ruleFile.rules(), local, directory);
            if (result != null) ignored = result;
        }
        return ignored;
    }

    void enterDirectory(Path directory) throws IOException {
        for (String name : List.of(".gitignore", ".codecompassignore")) {
            Path file = directory.resolve(name);
            if (Files.isRegularFile(file)) ruleFiles.add(new RuleFile(directory, parse(Files.readString(file, StandardCharsets.UTF_8))));
        }
    }

    void leaveDirectory(Path directory) {
        ruleFiles.removeIf(ruleFile -> ruleFile.base().equals(directory));
    }

    private static final java.util.Set<String> DEFAULT_IGNORED_DIRECTORIES = java.util.Set.of(
            ".git", ".hg", ".svn", ".idea", ".vscode", ".gradle", ".next", ".nuxt", ".svelte-kit",
            ".angular", ".vite", ".turbo", ".parcel-cache", ".cache", ".terraform", ".serverless",
            ".aws-sam", ".venv", ".pytest_cache", ".mypy_cache", ".ruff_cache", ".tox", ".nox",
            "node_modules", "bower_components", "vendor", "Pods", "Carthage", "DerivedData", "build",
            "target", "dist", "out", "bin", "obj", "coverage", "htmlcov", "test-results",
            "playwright-report", "storybook-static", "data", "datasets", "venv", "__pycache__",
            "site-packages", "dist-packages", "generated", "generated-sources", "generated-test-sources",
            "gen", "tmp", "temp", "logs");

    private static Boolean match(IgnoreNode node, String path, boolean directory) {
        return node.checkIgnored(path, directory);
    }

    private static boolean applies(IgnoreNode node, String path, boolean directory, boolean current) {
        Boolean result = match(node, path, directory);
        return result == null ? current : result;
    }

    private static IgnoreNode parse(String text) {
        IgnoreNode node = new IgnoreNode();
        try {
            node.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
            return node;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse built-in ignore rules", exception);
        }
    }

    private static String unix(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    private record RuleFile(Path base, IgnoreNode rules) { }
}
