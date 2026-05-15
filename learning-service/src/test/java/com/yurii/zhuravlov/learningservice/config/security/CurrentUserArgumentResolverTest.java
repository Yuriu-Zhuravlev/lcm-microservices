package com.yurii.zhuravlov.learningservice.config.security;

import com.yurii.zhuravlov.learningservice.config.annotation.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @Mock
    private MethodParameter parameter;

    @Test
    void supportsParameter_ShouldReturnTrue_WhenAnnotationAndTypeMatch() {
        when(parameter.hasParameterAnnotation(CurrentUser.class)).thenReturn(true);
        doReturn(Long.class).when(parameter).getParameterType();

        boolean supported = resolver.supportsParameter(parameter);

        assertTrue(supported);
    }

    @Test
    void resolveArgument_ShouldReturnUserId_WhenAuthenticationExists() {
        Long expectedUserId = 123L;
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(authentication.getDetails()).thenReturn(expectedUserId);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            Object result = resolver.resolveArgument(parameter, null, null, null);

            assertEquals(expectedUserId, result);
        }
    }

    @Test
    void resolveArgument_ShouldReturnNull_WhenAuthenticationExists_NoId() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(authentication.getDetails()).thenReturn(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            Object result = resolver.resolveArgument(parameter, null, null, null);

            assertNull(result);
        }
    }

    @Test
    void resolveArgument_ShouldReturnNull_WhenNoAuthentication() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            Object result = resolver.resolveArgument(parameter, null, null, null);

            assertNull(result);
        }
    }
}