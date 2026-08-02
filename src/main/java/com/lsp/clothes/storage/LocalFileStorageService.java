package com.lsp.clothes.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${clothes.storage.local-root:./data/uploads}")
    private String configuredRoot;
    private Path root;

    @PostConstruct
    public void initialize() throws IOException {
        root = Path.of(configuredRoot).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String save(InputStream input, String extension) throws IOException {
        LocalDate today = LocalDate.now();
        String directory = today.getYear() + "/" + String.format("%02d", today.getMonthValue());
        String storageKey = directory + "/" + UUID.randomUUID().toString().replace("-", "")
                + (extension.isBlank() ? "" : "." + extension);
        Path target = resolveSafe(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolveSafe(storageKey);
        if (!Files.isRegularFile(path)) throw new IllegalStateException("文件内容不存在");
        return new PathResource(path);
    }

    @Override
    public void deleteQuietly(String storageKey) {
        try { Files.deleteIfExists(resolveSafe(storageKey)); } catch (IOException ignored) { }
    }

    private Path resolveSafe(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) throw new IllegalArgumentException("存储键为空");
        Path path = root.resolve(storageKey.replace('\\', '/')).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("非法存储路径");
        return path;
    }
}
