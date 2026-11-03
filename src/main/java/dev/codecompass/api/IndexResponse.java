package dev.codecompass.api;

public record IndexResponse(String repository, int filesIndexed, int chunksIndexed, boolean alreadyIndexed) { }
