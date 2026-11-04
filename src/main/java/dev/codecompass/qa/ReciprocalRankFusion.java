package dev.codecompass.qa;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReciprocalRankFusion {
    private static final int RANK_SMOOTHING = 60;

    public List<Document> fuse(RankedCandidates candidates) {
        Map<String, FusedDocument> fused = new LinkedHashMap<>();
        for (RankedCandidates.RankedList list : candidates.lists()) {
            for (int index = 0; index < list.documents().size(); index++) {
                Document document = list.documents().get(index);
                String key = identity(document);
                double contribution = list.weight() / (RANK_SMOOTHING + index + 1.0);
                fused.compute(key, (ignored, prior) -> prior == null
                        ? new FusedDocument(document, contribution)
                        : new FusedDocument(prior.document(), prior.score() + contribution));
            }
        }

        return fused.values().stream()
                .sorted(Comparator.comparingDouble(FusedDocument::score).reversed())
                .map(this::withScore).toList();
    }

    private String identity(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return metadata.get("path") + ":" + metadata.get("startLine") + ":" + metadata.get("endLine");
    }

    private Document withScore(FusedDocument fused) {
        Map<String, Object> metadata = new HashMap<>(fused.document().getMetadata());
        metadata.put("ragScore", fused.score());
        return new Document(fused.document().getText(), metadata);
    }

    private record FusedDocument(Document document, double score) { }
}
