package dev.codecompass.qa;

import org.springframework.ai.document.Document;

import java.util.List;

public record RankedCandidates(List<RankedList> lists) {
    public record RankedList(String channel, double weight, List<Document> documents) { }
}
