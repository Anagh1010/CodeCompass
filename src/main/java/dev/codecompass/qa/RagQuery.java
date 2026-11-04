package dev.codecompass.qa;

import java.util.List;

public record RagQuery(String original, String semanticQuery, List<String> terms,
                       List<String> exactIdentifiers, boolean startupIntent) { }
