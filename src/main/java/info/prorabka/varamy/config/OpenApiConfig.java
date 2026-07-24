package info.prorabka.varamy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("katok.pro")
                        .description("API для хоккейного приложения Vara. Позволяет управлять пользователями, профилями игроков, объявлениями и откликами.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("katok.pro")
                                .url("http://varatut.ru"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server().url("https://varamy.online").description("БД"),
                        new Server().url("http://192.168.0.119:" + serverPort).description("Локальный сервер")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT токен в формате: Bearer {token}")));
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }
}
