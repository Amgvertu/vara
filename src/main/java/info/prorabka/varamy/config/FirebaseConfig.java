package info.prorabka.varamy.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {
    @PostConstruct
    public void initialize() throws IOException {
        log.info("🔍 Проверяем наличие firebase-adminsdk.json");
        log.info("📁 Текущая папка: {}", new File(".").getAbsolutePath());

        ClassPathResource resource = new ClassPathResource("firebase-adminsdk.json");

        if (!resource.exists()) {
            log.error("❌ firebase-adminsdk.json НЕ НАЙДЕН!");
            throw new IOException("Файл firebase-adminsdk.json не найден");
        }

        log.info("✅ firebase-adminsdk.json найден");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .setConnectTimeout(5000)
                .setReadTimeout(5000)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase успешно инициализирован");
        } else {
            log.info("ℹ️ Firebase уже инициализирован");
        }
    }
}