package info.prorabka.varamy.config;

import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sms.gateway.user-id}")
    private String gatewayUserId;

    @Override
    public void run(String... args) {
        UUID id = UUID.fromString(gatewayUserId);
        if (userRepository.findById(id).isEmpty()) {
            User gateway = new User();
            gateway.setId(id);
            gateway.setPhone("+79999999999");
            gateway.setPasswordHash(passwordEncoder.encode("gateway123"));
            gateway.setRole(User.UserRole.SMS_GATEWAY);
            gateway.setStatus(User.UserStatus.ACTIVE);
            gateway.setRegisteredAt(java.time.LocalDateTime.now());
            userRepository.save(gateway);
            log.info("✅ Создан пользователь SMS_GATEWAY с ID: {}", gatewayUserId);
        } else {
            log.info("ℹ️ Пользователь SMS_GATEWAY уже существует");
        }
    }
}
