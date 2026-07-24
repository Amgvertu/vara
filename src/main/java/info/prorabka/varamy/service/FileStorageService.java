package info.prorabka.varamy.service;

import info.prorabka.varamy.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String storeFile(MultipartFile file, String subDir) {
        try {
            if (file.isEmpty()) {
                throw new BadRequestException("Файл пуст");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID() + extension;

            Path uploadPath = Paths.get(uploadDir, subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return baseUrl + "/files/" + subDir + "/" + filename;

        } catch (IOException e) {
            log.error("Ошибка при сохранении файла", e);
            throw new RuntimeException("Ошибка при сохранении файла", e);
        }
    }

    /**
     * Удаляет файл по его публичному URL.
     * @param fileUrl полный URL файла (например, https://92.125.255.63/api2/files/avatars/uuid.jpg)
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            // Ищем маркер "/files/" в URL
            int filesIndex = fileUrl.indexOf("/files/");
            if (filesIndex == -1) {
                log.warn("Невозможно удалить файл: URL не содержит '/files/': {}", fileUrl);
                return;
            }
            // Извлекаем относительный путь после "/files/"
            String relativePath = fileUrl.substring(filesIndex + 7); // 7 = длина "/files/"
            // Формируем полный путь к файлу на диске
            Path filePath = Paths.get(uploadDir).resolve(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Удалён старый аватар: {}", filePath);
            } else {
                log.debug("Файл для удаления не найден: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Ошибка при удалении файла: {}", e.getMessage());
        }
    }
}
