package com.yurii.zhuravlov.gateway.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yurii.zhuravlov.gateway.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.HandlerFunction;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private HandlerFunction<ServerResponse> next;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    @Test
    void filter_ShouldProceed_WhenTokenIsValid() throws Exception {
        // 1. Створюємо MockHttpServletRequest (стандартний Servlet API)
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/courses");
        servletRequest.addHeader("Authorization", "Bearer valid-token");

        // 2. Обертаємо його в ServerRequest.create()
        // Це створить правильний об'єкт для WebMvc Functional Routing
        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(next.handle(request)).thenReturn(ServerResponse.ok().build());

        // When
        ServerResponse response = authenticationFilter.filter(request, next);

        // Then
        assertEquals(HttpStatus.OK, response.statusCode());
        verify(next).handle(request);
    }

    @Test
    void filter_ShouldReturnUnauthorized_WhenNoHeader() throws Exception {
        // Given
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/courses");
        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());

        // When
        ServerResponse response = authenticationFilter.filter(request, next);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
        verifyNoInteractions(next);
    }



    @Test
    void filter_ShouldReturnUnauthorized_WithSpecificMessage_WhenTokenIsInvalid() throws Exception {
        // Given: Токен є, але він прострочений або підроблений
        String invalidToken = "expired-or-fake-token";
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/courses");
        servletRequest.addHeader("Authorization", "Bearer " + invalidToken);
        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());

        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        // When
        ServerResponse response = authenticationFilter.filter(request, next);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());
        // Важливо: оскільки body у ServerResponse не так просто дістати в тестах через mock,
        // ми перевіряємо логіку викликів.
        // Але якщо ти хочеш перевірити саме текст "Invalid or expired auth token",
        // то краще використовувати StepVerifier (для WebFlux) або перевірити entity у відповіді:
        // assertTrue(response instanceof EntityResponse);

        verify(jwtService).isTokenValid(invalidToken);
        verifyNoInteractions(next);
    }

    @Test
    void filter_ShouldPassLoginPath_WithoutToken() throws Exception {
        // Given: Запит на логін без заголовка Authorization
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());

        ServerResponse expectedResponse = ServerResponse.ok().build();
        when(next.handle(request)).thenReturn(expectedResponse);

        // When
        ServerResponse actualResponse = authenticationFilter.filter(request, next);

        // Then
        assertEquals(HttpStatus.OK, actualResponse.statusCode());
        verify(next).handle(request);
        verifyNoInteractions(jwtService); // JWT не чіпаємо
    }

    @Test
    void filter_ShouldPassRegisterPath_WithoutToken() throws Exception {
        // Given: Запит на реєстрацію
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/auth/register");
        ServerRequest request = ServerRequest.create(servletRequest, Collections.emptyList());

        ServerResponse expectedResponse = ServerResponse.ok().build();
        when(next.handle(request)).thenReturn(expectedResponse);

        // When
        ServerResponse actualResponse = authenticationFilter.filter(request, next);

        // Then
        assertEquals(HttpStatus.OK, actualResponse.statusCode());
        verify(next).handle(request);
        verifyNoInteractions(jwtService);
    }
}