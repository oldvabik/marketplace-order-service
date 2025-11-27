package org.oldvabik.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.oldvabik.orderservice.config.UserServiceProperties;
import org.oldvabik.orderservice.dto.UserDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class UserServiceClient {
    private final RestTemplate restTemplate;
    private final UserServiceProperties properties;

    public UserServiceClient(RestTemplate restTemplate,
                             UserServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserDto getUserByEmail(Authentication auth, String email) {
        String token = (String) auth.getCredentials();

        String url = UriComponentsBuilder.fromUriString(properties.getUrl())
                .path(properties.getEndpoints().getGetUserByEmail())
                .queryParam("email", email)
                .toUriString();

        return callUserService(url, token);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(Authentication auth, Long id) {
        String token = (String) auth.getCredentials();

        String url = UriComponentsBuilder.fromUriString(properties.getUrl())
                .path(properties.getEndpoints().getGetUserById())
                .buildAndExpand(id)
                .toUriString();

        return callUserService(url, token);
    }

    private UserDto callUserService(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, UserDto.class).getBody();
    }

    public UserDto getUserByEmailFallback(Authentication auth, String email, Throwable t) {
        log.warn("Fallback triggered for getUserByEmail(email={})", email);
        return UserDto.builder()
                .id(-1L)
                .email(email)
                .name("unknown")
                .surname("unknown")
                .build();
    }

    public UserDto getUserByIdFallback(Authentication auth, Long id, Throwable t) {
        log.warn("Fallback triggered for getUserById(id={})", id);
        return UserDto.builder()
                .id(id)
                .email("unknown@gmail.com")
                .name("unknown")
                .surname("unknown")
                .build();
    }
}
