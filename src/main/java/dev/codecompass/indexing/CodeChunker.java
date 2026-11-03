package dev.codecompass.indexing;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CodeChunker {
    private static final int MAX_CHUNK_CHARACTERS = 2_500;
    private static final Pattern DECLARATION = Pattern.compile(
            "^(\\s*(?:export\\s+|default\\s+|public\\s+|private\\s+|protected\\s+|static\\s+|async\\s+|abstract\\s+|final\\s+|open\\s+|override\\s+|sealed\\s+|data\\s+|suspend\\s+|inline\\s+|virtual\\s+|func\\s+|def\\s+|class\\s+|interface\\s+|enum\\s+|record\\s+|struct\\s+|trait\\s+|type\\s+|object\\s+|const\\s+|let\\s+|var\\s+|fun\\s+|@))");

    public List<Document> split(String content, String path) {
        String[] lines = content.split("\\n", -1);
        List<Document> chunks = new ArrayList<>();
        int start = 0;
        while (start < lines.length) {
            int end = findChunkEnd(lines, start);
            String text = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
            if (!text.isBlank()) {
                chunks.add(new Document(text, Map.of("path", path, "startLine", start + 1, "endLine", end)));
            }
            start = end;
        }
        return chunks;
    }

    private int findChunkEnd(String[] lines, int start) {
        int cursor = start;
        int characters = 0;
        int lastDeclaration = -1;
        while (cursor < lines.length) {
            int added = lines[cursor].length() + 1;
            if (characters + added > MAX_CHUNK_CHARACTERS && cursor > start) {
                if (lastDeclaration > start && lastDeclaration >= start + (cursor - start) / 2) return lastDeclaration;
                return cursor;
            }
            characters += added;
            if (cursor > start && DECLARATION.matcher(lines[cursor]).find()) lastDeclaration = cursor;
            cursor++;
        }
        return cursor;
    }
}
