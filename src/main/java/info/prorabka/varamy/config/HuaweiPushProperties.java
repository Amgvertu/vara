package info.prorabka.varamy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "huawei.push")
public class HuaweiPushProperties {
    private String appId;
    private String clientSecret;
    private String clientId;
    private String tokenUrl;
    private String pushUrl;
}