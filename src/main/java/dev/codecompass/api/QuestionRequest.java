package dev.codecompass.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QuestionRequest(@NotBlank String question,
                              @NotBlank @Pattern(regexp = "ollama|gemini") String provider) { }
