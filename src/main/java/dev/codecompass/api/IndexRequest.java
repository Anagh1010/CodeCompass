package dev.codecompass.api;

import jakarta.validation.constraints.NotBlank;

public record IndexRequest(@NotBlank String path, boolean reuseExisting) { }
