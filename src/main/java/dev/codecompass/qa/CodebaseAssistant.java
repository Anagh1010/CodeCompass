package dev.codecompass.qa;

import dev.codecompass.api.AnswerResponse;
import org.springframework.stereotype.Service;

@Service
public class CodebaseAssistant {
    private final RagPipeline ragPipeline;

    public CodebaseAssistant(RagPipeline ragPipeline) {
        this.ragPipeline = ragPipeline;
    }

    public AnswerResponse ask(String question, String provider) {
        return ragPipeline.answer(question, provider);
    }
}
