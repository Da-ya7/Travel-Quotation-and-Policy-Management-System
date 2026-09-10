package com.company.travel.document.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final Path root;

    public LocalDocumentStorage(@Value("${document.storage.local-root:./document-storage}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    @Override
    public String store(String fileName, byte[] content) throws IOException {
        Files.createDirectories(root);
        String storageUri = UUID.randomUUID() + "-" + Path.of(fileName).getFileName();
        Files.write(root.resolve(storageUri), content);
        return storageUri;
    }

    @Override
    public byte[] retrieve(String storageUri) throws IOException {
        Path resolved = root.resolve(storageUri).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Invalid storage URI");
        }
        return Files.readAllBytes(resolved);
    }
}