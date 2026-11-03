package dev.codecompass.api;

import java.util.List;

public record AnswerResponse(String answer, List<SourceReference> sources) {
    public record SourceReference(String path, int line) { }
}
