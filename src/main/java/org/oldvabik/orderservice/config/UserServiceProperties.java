package org.oldvabik.orderservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "user.service")
public class UserServiceProperties {
    private String url;
    private Endpoints endpoints = new Endpoints();

    @Data
    public static class Endpoints {
        private String getUserById;
        private String getUserByEmail;
    }
}
