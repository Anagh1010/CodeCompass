package dev.codecompass.qa;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Prevents generation when retrieved chunks do not mention the question's subject. */
@Component
public class RagEvidenceGuard {
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)]");
    private static final Set<String> GENERIC_TERMS = Set.of(
            "what", "where", "when", "which", "who", "why", "how", "does", "that", "this", "with", "from",
            "into", "being", "used", "using", "use", "main", "application", "project", "codebase", "file",
            "app", "system", "component", "components", "frontend", "backend", "process", "processing",
            "handle", "handling", "manage", "manages", "responsible", "data", "information", "user", "users",
            "service", "services", "class", "method", "function", "feature", "features", "work", "works",
            "include", "includes", "including", "implement", "implemented", "implementation", "support", "supports");

    public boolean hasRelevantEvidence(RagQuery query, List<Document> sources) {
        return !relevantSourceIndexes(query, sources).isEmpty();
    }

    public List<Integer> relevantSourceIndexes(RagQuery query, List<Document> sources) {
        List<Integer> matches = new java.util.ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            Document source = sources.get(index);
            Set<String> sourceTerms = new HashSet<>();
            tokenize(source.getText(), sourceTerms);
            tokenize(String.valueOf(source.getMetadata().get("path")), sourceTerms);
            boolean identifierMatch = query.exactIdentifiers().stream()
                    .filter(RagEvidenceGuard::isTechnicalIdentifier)
                    .map(RagEvidenceGuard::tokens)
                    .anyMatch(sourceTerms::containsAll);
            boolean termMatch = query.terms().stream()
                    .filter(term -> term.length() >= 4 && !GENERIC_TERMS.contains(term.toLowerCase(Locale.ROOT)))
                    .map(RagEvidenceGuard::canonical)
                    .anyMatch(sourceTerms::contains);
            if (identifierMatch || termMatch) matches.add(index + 1);
        }
        return List.copyOf(matches);
    }

    public boolean hasValidCitation(String answer, int sourceCount) {
        Matcher matcher = CITATION.matcher(answer);
        boolean found = false;
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index < 1 || index > sourceCount) return false;
            found = true;
        }
        return found;
    }

    public boolean hasCitationMarker(String answer) {
        return CITATION.matcher(answer).find();
    }

    private static void tokenize(String text, Set<String> tokens) {
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            String expanded = matcher.group().replaceAll("([a-z0-9])([A-Z])", "$1 $2");
            for (String part : expanded.split("\\s+")) {
                String token = canonical(part.toLowerCase(Locale.ROOT));
                if (token.length() >= 2) tokens.add(token);
            }
        }
    }

    private static Set<String> tokens(String text) {
        Set<String> tokens = new HashSet<>();
        tokenize(text, tokens);
        return tokens;
    }

    private static boolean isTechnicalIdentifier(String identifier) {
        return identifier.contains("_") || identifier.contains("$")
                || identifier.matches(".*[a-z][A-Z].*")
                || identifier.matches("[A-Z0-9]{2,}");
    }

    private static String canonical(String token) {
        if (token.length() > 4 && token.endsWith("ies")) return token.substring(0, token.length() - 3) + "y";
        if (token.length() > 4 && token.endsWith("s") && !token.endsWith("ss")) return token.substring(0, token.length() - 1);
        return token;
    }
}
