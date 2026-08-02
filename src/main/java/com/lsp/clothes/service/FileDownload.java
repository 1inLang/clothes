package com.lsp.clothes.service;

import org.springframework.core.io.Resource;

public record FileDownload(Resource resource, String fileName, String mimeType, long fileSize) {
}
