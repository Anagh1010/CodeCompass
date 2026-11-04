package dev.codecompass.qa;

import org.springframework.ai.document.Document;

import java.util.List;

public record RagContext(List<Document> sources, String formatted) { }
