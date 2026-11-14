package com.example.pos_system.security;

import com.example.pos_system.model.service.auth.AuthCookies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class TokenResolver {

    /** If the "Authorization" header is present → use it. If not → use the Cookie. */
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null) {
            return header.startsWith("Bearer ") ? header.substring(7) : null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AuthCookies.NAME.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
