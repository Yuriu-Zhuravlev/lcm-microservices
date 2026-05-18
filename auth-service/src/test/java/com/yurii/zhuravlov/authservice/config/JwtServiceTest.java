package com.yurii.zhuravlov.authservice.config;

import com.yurii.zhuravlov.authservice.service.userdetails.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    private final String secret = "Zm9yLWV4YW1wbGUtc2VjcmV0LWtleS1tdXN0LWJlLXZlcnktbG9uZw==";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        long expiration = 3600000;
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expiration);
    }

    @Test
    void generateToken_ShouldContainCorrectClaims() {
        Long expectedUserId = 123L;
        String username = "testUser";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        CustomUserDetails userDetails = new CustomUserDetails(username, "pass", authorities, expectedUserId);

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertEquals(username, jwtService.extractUsername(token));

        assertEquals(expectedUserId, jwtService.extractUserId(token));

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<?> roles = claims.get("roles", List.class);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenUserMatchAndNotExpired() {
        String username = "yurii";
        CustomUserDetails userDetails = new CustomUserDetails(username, "pass", List.of(), 1L);
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        CustomUserDetails userInToken = new CustomUserDetails("user1", "pass", List.of(), 1L);
        CustomUserDetails otherUser = new CustomUserDetails("user2", "pass", List.of(), 2L);
        String token = jwtService.generateToken(userInToken);

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertFalse(isValid);
    }
}