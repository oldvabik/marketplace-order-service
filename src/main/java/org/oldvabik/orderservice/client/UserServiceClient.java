package org.oldvabik.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.oldvabik.orderservice.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class UserServiceClient {
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate,
                             @Value("${user.service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserDto getUserByEmail(Authentication auth, String email) {
        String token = (String) auth.getCredentials();
        String url = userServiceUrl + "/api/v1/users/search?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, UserDto.class).getBody();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(Authentication auth, Long id) {
        String token = (String) auth.getCredentials();
        String url = userServiceUrl + "/api/v1/users/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, UserDto.class).getBody();
    }

    public UserDto getUserByEmailFallback(Authentication auth, String email, Throwable throwable) {
        UserDto fallbackUser = new UserDto();
        fallbackUser.setId(-1L);
        fallbackUser.setName("Unknown");
        fallbackUser.setSurname("Unknown");
        fallbackUser.setEmail(email);
        return fallbackUser;
    }

    public UserDto getUserByIdFallback(Authentication auth, Long id, Throwable throwable) {
        UserDto fallbackUser = new UserDto();
        fallbackUser.setId(id);
        fallbackUser.setName("Unknown");
        fallbackUser.setSurname("Unknown");
        fallbackUser.setEmail("unknown@gmail.com");
        return fallbackUser;
    }
}
