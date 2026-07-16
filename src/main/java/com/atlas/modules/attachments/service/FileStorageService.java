package com.atlas.modules.attachments.service;

import com.atlas.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${atlas.uploads.dir:uploads}") String uploadDir) throws IOException {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
    }

    public String store(MultipartFile file) throws IOException {
        String storageKey = UUID.randomUUID() + "_" + sanitizeFileName(file.getOriginalFilename());
        Path target = uploadRoot.resolve(storageKey).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IOException("Invalid storage path");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    public Resource loadAsResource(String storageKey) {
        try {
            Path file = uploadRoot.resolve(storageKey).normalize();
            if (!file.startsWith(uploadRoot)) {
                throw new ResourceNotFoundException("File not found");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
