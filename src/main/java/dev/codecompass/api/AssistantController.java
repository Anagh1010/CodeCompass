package dev.codecompass.api;

import dev.codecompass.indexing.RepositoryIndexer;
import dev.codecompass.qa.CodebaseAssistant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class AssistantController {
    private final RepositoryIndexer indexer;
    private final CodebaseAssistant assistant;

    public AssistantController(RepositoryIndexer indexer, CodebaseAssistant assistant) {
        this.indexer = indexer;
        this.assistant = assistant;
    }

    @PostMapping("/index")
    public IndexResponse index(@Valid @RequestBody IndexRequest request) throws IOException {
        var result = indexer.index(request.path(), request.reuseExisting());
        return new IndexResponse(result.repository(), result.files(), result.chunks(), result.alreadyIndexed());
    }

    @PostMapping("/ask")
    public AnswerResponse ask(@Valid @RequestBody QuestionRequest request) {
        return assistant.ask(request.question(), request.provider());
    }
}
