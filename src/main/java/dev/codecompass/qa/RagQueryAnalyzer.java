package dev.codecompass.qa;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RagQueryAnalyzer {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "from", "does", "where", "what", "how", "this", "that", "are", "was", "were", "into", "being", "used", "using", "use", "which", "main", "application", "project", "codebase", "file");

    public RagQuery analyze(String question) {
        Set<String> terms = new LinkedHashSet<>();
        Set<String> identifiers = new LinkedHashSet<>();
        Matcher matcher = IDENTIFIER.matcher(question);
        while (matcher.find()) {
            String token = matcher.group();
            if (hasIdentifierShape(token)) identifiers.add(token);
            String expanded = token.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
            for (String part : expanded.split("\\s+")) {
                String normalized = part.toLowerCase(Locale.ROOT);
                if (normalized.length() >= 2 && !STOP_WORDS.contains(normalized)) terms.add(normalized);
            }
        }
        List<String> normalizedTerms = new ArrayList<>(terms);
        boolean startup = hasAny(question.toLowerCase(Locale.ROOT), "entry point", "entrypoint", "application start", "app start", "start the application", "start the app", "run the application", "run the app", "launch the application", "launch the app");
        String semanticQuery = question + (normalizedTerms.isEmpty() ? "" : " " + String.join(" ", normalizedTerms));
        return new RagQuery(question, semanticQuery, normalizedTerms, new ArrayList<>(identifiers), startup);
    }

    private boolean hasIdentifierShape(String token) {
        return token.contains("_") || token.contains("$") || token.chars().anyMatch(Character::isUpperCase);
    }

    private boolean hasAny(String text, String... phrases) {
        for (String phrase : phrases) if (text.contains(phrase)) return true;
        return false;
    }
}
