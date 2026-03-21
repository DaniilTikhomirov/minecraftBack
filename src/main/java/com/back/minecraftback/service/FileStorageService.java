package com.back.minecraftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final Tika tika;

    /**
     * Сюда Spring подставляет из application.properties
     * или из ENV STORAGE_DIR
     */
    @Value("${storage.dir:}")
    private String storageDir;

    private Path storageRoot;

    /**
     * Инициализация хранилища
     */
    @PostConstruct
    public void initStorage() {
        try {
            if (storageDir == null || storageDir.isBlank()) {
                // Если путь не задан → используем домашнюю папку пользователя
                String userHome = System.getProperty("user.home");
                storageDir = userHome + "/minecraft_photo_data";
            }

            storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();

            if (!Files.exists(storageRoot)) {
                Files.createDirectories(storageRoot);
            }

            log.info("File storage initialized at: {}", storageRoot);

        } catch (IOException e) {
            throw new RuntimeException("Не удалось инициализировать директорию хранилища", e);
        }
    }

    public String save(byte[] data) {
        try {
            String extension = getExtension(detectType(data));
            String fileName = UUID.randomUUID() + extension;

            Path filePath = storageRoot.resolve(fileName);
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл", e);
        }
    }

    public String save(byte[] data, String inputPath) {
        try {
            Path filePath = storageRoot.resolve(inputPath).normalize();

            if (!filePath.startsWith(storageRoot)) {
                throw new RuntimeException("Попытка сохранения вне хранилища");
            }

            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return storageRoot.relativize(filePath)
                    .toString()
                    .replace(filePath.getFileSystem().getSeparator(), "/");

        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл: " + inputPath, e);
        }
    }

    public FileSystemResource loadAsResource(String fileName) {
        try {
            Path filePath = storageRoot.resolve(fileName).normalize();

            if (!filePath.startsWith(storageRoot)) {
                throw new RuntimeException("Попытка доступа вне хранилища");
            }

            if (!Files.exists(filePath)) {
                throw new RuntimeException("Файл не найден: " + fileName);
            }

            return new FileSystemResource(filePath.toFile());
        } catch (Exception e) {
            throw new RuntimeException("Не удалось прочитать файл", e);
        }
    }

    /**
     * Удаляет файл внутри {@link #storageRoot} по относительному пути из БД. Безопасно при подставленном из БД значении:
     * нормализация и проверка, что путь не выходит за пределы хранилища.
     */
    public void deleteStoredFileIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = storageRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(storageRoot)) {
                log.warn("Skip delete: path escapes storage root: {}", relativePath);
                return;
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete stored file {}: {}", relativePath, e.getMessage());
        }
    }

    private String detectType(byte[] data) {
        return tika.detect(data);
    }

    private String getExtension(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }
}
