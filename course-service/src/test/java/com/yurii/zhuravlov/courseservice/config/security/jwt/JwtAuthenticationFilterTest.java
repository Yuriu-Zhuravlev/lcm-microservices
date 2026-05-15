package com.yurii.zhuravlov.courseservice.config.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_Success_WithValidToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/courses");

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("yurii");
        when(jwtService.extractUserId(token)).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("yurii", auth.getName());
        assertEquals(1L, auth.getDetails());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_Success_WithInvalidToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        request.setServletPath("/api/courses");

        when(jwtService.isTokenValid(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_Forbidden_WhenInternalAccessWithoutHeader() throws ServletException, IOException {
        request.setRequestURI("/api/courses/lessons/internal/1");
        request.setServletPath("/api/courses/lessons/internal/1");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_Forbidden_WhenInternalAccessWithIncorrectHeader() throws ServletException, IOException {
        request.setRequestURI("/api/courses/lessons/internal/1");
        request.setServletPath("/api/courses/lessons/internal/1");
        request.addHeader("X-Internal-Service", "wrong-service");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_Success_WhenInternalAccessWithCorrectHeader() throws ServletException, IOException {
        request.setRequestURI("/api/courses/lessons/internal/1");
        request.setServletPath("/api/courses/lessons/internal/1");
        request.addHeader("X-Internal-Service", "learning-service");
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn("yurii");
        when(jwtService.extractUserId(token)).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("yurii", auth.getName());
        assertEquals(1L, auth.getDetails());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_Skip_ForSwagger() throws ServletException, IOException {
        request.setServletPath("/v3/api-docs");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_Continue_WhenNoAuthHeader() throws ServletException, IOException {
        request.setServletPath("/api/courses");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_Continue_WhenNotBearerAuthHeader() throws ServletException, IOException {
        request.setServletPath("/api/courses");
        String token = "valid.jwt.token";
        request.addHeader("Authorization", token);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}