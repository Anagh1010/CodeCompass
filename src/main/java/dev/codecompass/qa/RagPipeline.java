package dev.codecompass.qa;

import dev.codecompass.api.AnswerResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagPipeline {
    private static final String NO_EVIDENCE = "I couldn't verify this from the indexed code. Try re-indexing or ask about a specific symbol, file, or behavior.";
    private static final String NO_CITED_ANSWER = "I found related code, but couldn't produce an answer with verifiable citations. Try asking about a specific class or method.";

    private final RagQueryAnalyzer queryAnalyzer;
    private final HybridRetriever retriever;
    private final ReciprocalRankFusion fusion;
    private final CodeChunkReranker reranker;
    private final RagContextBuilder contextBuilder;
    private final GroundedAnswerGenerator answerGenerator;
    private final RagEvidenceGuard evidenceGuard;

    public RagPipeline(RagQueryAnalyzer queryAnalyzer, HybridRetriever retriever,
                       ReciprocalRankFusion fusion, CodeChunkReranker reranker,
                       RagContextBuilder contextBuilder, GroundedAnswerGenerator answerGenerator,
                       RagEvidenceGuard evidenceGuard) {
        this.queryAnalyzer = queryAnalyzer;
        this.retriever = retriever;
        this.fusion = fusion;
        this.reranker = reranker;
        this.contextBuilder = contextBuilder;
        this.answerGenerator = answerGenerator;
        this.evidenceGuard = evidenceGuard;
    }

    public AnswerResponse answer(String question, String provider) {
        RagQuery query = queryAnalyzer.analyze(question);
        RankedCandidates candidates = retriever.retrieve(query);
        List<Document> fused = fusion.fuse(candidates);
        List<Document> ranked = reranker.rerank(query, fused);
        RagContext context = contextBuilder.build(ranked);
        List<Integer> relevantSources = evidenceGuard.relevantSourceIndexes(query, context.sources());
        if (relevantSources.isEmpty()) {
            return new AnswerResponse(NO_EVIDENCE, List.of());
        }

        String answer = answerGenerator.generate(query, context, provider);
        if (!evidenceGuard.hasValidCitation(answer, context.sources().size())) {
            if (evidenceGuard.hasCitationMarker(answer)) {
                return new AnswerResponse(NO_CITED_ANSWER, List.of());
            }
            answer = answer.stripTrailing() + "\n\nSources: " + citationList(relevantSources);
        }
        return new AnswerResponse(answer, sourceReferences(context.sources()));
    }

    private String citationList(List<Integer> indexes) {
        return indexes.stream().limit(3).map(index -> "[" + index + "]")
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private List<AnswerResponse.SourceReference> sourceReferences(List<Document> sources) {
        List<AnswerResponse.SourceReference> references = new ArrayList<>();
        for (Document document : sources) {
            references.add(new AnswerResponse.SourceReference(
                    String.valueOf(document.getMetadata().get("path")),
                    line(document, "startLine"), line(document, "endLine")));
        }
        return references;
    }

    private int line(Document document, String name) {
        Object value = document.getMetadata().get(name);
        return value instanceof Number number ? number.intValue() : 1;
    }
}
