package info.prorabka.varamy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private long accessTokenExpiration = 1800000; // 30 минут
    private long refreshTokenExpiration = 604800000; // 7 дней
    private String issuer = "varamy";
}