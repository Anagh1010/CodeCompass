package dev.codecompass.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class VectorStoreConfiguration {
    @Bean
    SimpleVectorStore vectorStore(EmbeddingModel embeddingModel,
                                  @Value("${codecompass.vector-file:./data/ollama-code-vectors.json}") String vectorFile) throws IOException {
        Path file = Path.of(vectorFile);
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        if (Files.exists(file)) vectorStore.load(file.toFile());
        return vectorStore;
    }
}
