package com.lsp.clothes.storage;

import org.springframework.core.io.Resource;
import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {
    String save(InputStream input, String extension) throws IOException;
    Resource load(String storageKey);
    void deleteQuietly(String storageKey);
}
