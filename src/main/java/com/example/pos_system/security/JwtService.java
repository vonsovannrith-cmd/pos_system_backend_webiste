package com.example.pos_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    // convert BASE64 to key for (HMAC-SHA)
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // generate token (email, role, issuedAt, expiration, signWith )
    public String generateToken(UserDetails user) {

        String role = user.getAuthorities().iterator().next().getAuthority();
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    // find Username
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    // check token is expiration or not?
    public boolean isValid(String token, UserDetails user) {
        Claims claims = parse(token);
        return claims.getSubject().equals(user.getUsername())
                && claims.getExpiration().after(new Date());
    }

    // check signWith if fack token and expiration token will show throw exception
    private Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // show error when you not define secret key
    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank() || secret.startsWith("PASTE_YOUR")) {
            throw new IllegalStateException(
                    "\n" +
                            "JWT_SECRET is not yet set. Generate it using: openssl rand -base64 32");
        }
        getKey(); // Throw an exception if it is not Base64 or is shorter than 256 bits
    }
}
