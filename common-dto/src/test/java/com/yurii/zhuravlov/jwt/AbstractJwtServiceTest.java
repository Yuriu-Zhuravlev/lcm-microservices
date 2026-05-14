package com.yurii.zhuravlov.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AbstractJwtServiceTest {
    private TestJwtService jwtService;
    private final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static class TestJwtService extends AbstractJwtService {
        private final String key;
        TestJwtService(String key) { this.key = key; }
        @Override
        protected String getSecretKey() { return key; }
    }

    @BeforeEach
    void setUp() {
        jwtService = new TestJwtService(SECRET);
    }

    @Test
    void extractUserId_ShouldWorkCorrect() {
        Long expectedId = 123L;
        String token = Jwts.builder()
                .claim("userId", expectedId)
                .subject("testUser")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        Long actualId = jwtService.extractUserId(token);

        assertEquals(expectedId, actualId);
    }

    @Test
    void extractUsername_ShouldWorkCorrect() {
        Long expectedId = 123L;
        String username = "testUser";
        String token = Jwts.builder()
                .claim("userId", expectedId)
                .subject("testUser")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        String usernameActual = jwtService.extractUsername(token);

        assertEquals(username, usernameActual);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenInvalid() {
        assertFalse(jwtService.isTokenValid("invalid.token.here"));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenIsExpired() {
        // Створюємо токен, який закінчився 1 годину тому
        String expiredToken = Jwts.builder()
                .subject("testUser")
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        boolean isValid = jwtService.isTokenValid(expiredToken);

        assertFalse(isValid, "Сервіс мав визнати прострочений токен невалідним");
    }

    @Test
    void extractUserId_ShouldThrowException_WhenSignatureIsInvalid() {
        // Створюємо токен з іншим секретним ключем
        String badSecret = "AnotherVerySecretKeyForTestingPurposes1234567890123456";
        String encodedBadSecret = Base64.getEncoder().encodeToString(badSecret.getBytes());

        String tamperedToken = Jwts.builder()
                .claim("userId", 123L)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedBadSecret)))
                .compact();

        // Наш сервіс використовує свій SECRET, тому він має відхилити цей токен
        assertThrows(Exception.class, () -> jwtService.extractUserId(tamperedToken));
    }

    @Test
    void extractUserId_ShouldThrowException_WhenUserIdMissing() {
        String tokenWithoutId = Jwts.builder()
                .subject("testUser")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        // Тут залежить від того, як ти хочеш: отримати NullPointerException чи обробити це
        assertThrows(IllegalStateException.class, () -> jwtService.extractUserId(tokenWithoutId));
    }
}