package com.back.minecraftback.service;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final Tika tika;

    @Value("${storage.dir}")
    private String STORAGE_DIR;

    /**
     * Сохраняет файл с автоматическим именем и возвращает только имя файла
     */
    public String save(byte[] data) {
        try {
            String extension = getExtension(detectType(data));
            String fileName = UUID.randomUUID() + extension;

            Path storagePath = Paths.get(STORAGE_DIR).toAbsolutePath().normalize();
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            Path filePath = storagePath.resolve(fileName);
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return fileName;

        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл", e);
        }
    }

    /**
     * Сохраняет файл по указанной поддиректории относительно STORAGE_DIR
     * inputPath может быть типа "images/subdir/file.jpg"
     * Возвращает путь внутри storage (например "images/subdir/file.jpg")
     */
    public String save(byte[] data, String inputPath) {
        try {
            Path storagePath = Paths.get(STORAGE_DIR).toAbsolutePath().normalize();
            Path filePath = storagePath.resolve(inputPath).normalize();

            // защита от выхода за пределы storage
            if (!filePath.startsWith(storagePath)) {
                throw new RuntimeException("Попытка сохранения вне директории хранилища");
            }

            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // возвращаем путь относительно storage с нормализованными разделителями "/"
            return storagePath.relativize(filePath).toString().replace(filePath.getFileSystem().getSeparator(), "/");

        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл: " + inputPath, e);
        }
    }

    /**
     * Загружает файл относительно директории STORAGE_DIR
     */
    public FileSystemResource loadAsResource(String fileName) {
        try {
            Path storagePath = Paths.get(STORAGE_DIR).toAbsolutePath().normalize();
            Path filePath = storagePath.resolve(fileName).normalize();

            if (!filePath.startsWith(storagePath)) {
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
