package org.oldvabik.orderservice.security;

import org.oldvabik.orderservice.dto.UserDto;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AccessChecker {
    public boolean canAccessUser(Authentication auth, UserDto user) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }
        return auth.getName().equals(user.getEmail());
    }
}