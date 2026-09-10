package com.company.travel.document.storage;

import java.io.IOException;

public interface DocumentStorage {
    String store(String fileName, byte[] content) throws IOException;

    byte[] retrieve(String storageUri) throws IOException;
}