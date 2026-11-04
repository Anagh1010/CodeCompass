package dev.codecompass.qa;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RagContextBuilder {
    private static final int MAX_CONTEXT_CHARACTERS = 8_000;
    private static final int MAX_CHUNK_CHARACTERS = 2_200;
    private static final int MAX_CHUNKS = 6;
    private static final int MAX_CHUNKS_PER_FILE = 2;

    public RagContext build(List<Document> rankedDocuments) {
        List<Document> selected = new ArrayList<>();
        Map<String, Integer> fileCounts = new HashMap<>();
        Map<String, List<LineRange>> fileRanges = new HashMap<>();
        Set<String> seenChunks = new HashSet<>();
        int contextSize = 0;

        for (Document document : rankedDocuments) {
            if (selected.size() == MAX_CHUNKS || contextSize >= MAX_CONTEXT_CHARACTERS) break;
            String path = String.valueOf(document.getMetadata().get("path"));
            int startLine = line(document, "startLine");
            int endLine = line(document, "endLine");
            String identity = path + ":" + startLine + ":" + endLine;
            if (!seenChunks.add(identity) || fileCounts.getOrDefault(path, 0) >= MAX_CHUNKS_PER_FILE) continue;
            if (overlaps(fileRanges.getOrDefault(path, List.of()), startLine, endLine)) continue;

            int remaining = MAX_CONTEXT_CHARACTERS - contextSize;
            int allowance = Math.min(MAX_CHUNK_CHARACTERS, remaining);
            String excerpt = excerpt(document.getText(), allowance);
            if (excerpt.isBlank()) continue;
            Map<String, Object> metadata = new HashMap<>(document.getMetadata());
            int excerptEndLine = startLine + (int) excerpt.chars().filter(character -> character == '\n').count();
            metadata.put("endLine", excerptEndLine);
            selected.add(new Document(excerpt, metadata));
            contextSize += excerpt.length() + path.length() + 28;
            fileCounts.merge(path, 1, Integer::sum);
            fileRanges.computeIfAbsent(path, ignored -> new ArrayList<>()).add(new LineRange(startLine, excerptEndLine));
        }

        return new RagContext(List.copyOf(selected), format(selected));
    }

    private boolean overlaps(List<LineRange> ranges, int start, int end) {
        return ranges.stream().anyMatch(range -> start <= range.end() && end >= range.start());
    }

    private int line(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value instanceof Number number ? number.intValue() : 1;
    }

    private String excerpt(String text, int allowance) {
        if (allowance <= 0) return "";
        if (text.length() <= allowance) return text;
        int boundary = text.lastIndexOf('\n', allowance);
        if (boundary < allowance / 2) boundary = allowance;
        return text.substring(0, boundary);
    }

    private String format(List<Document> sources) {
        List<String> excerpts = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            Document document = sources.get(index);
            excerpts.add("[" + (index + 1) + "] " + document.getMetadata().get("path") + ":"
                    + document.getMetadata().get("startLine") + "-" + document.getMetadata().get("endLine")
                    + "\n" + document.getText());
        }
        return String.join("\n\n", excerpts);
    }

    private record LineRange(int start, int end) { }
}
