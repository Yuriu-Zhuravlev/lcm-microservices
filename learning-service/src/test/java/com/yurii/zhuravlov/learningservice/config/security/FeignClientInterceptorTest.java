package com.yurii.zhuravlov.learningservice.config.security;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeignClientInterceptorTest {

    private final FeignClientInterceptor interceptor = new FeignClientInterceptor();

    @Mock
    private RequestTemplate requestTemplate;

    @Mock
    private ServletRequestAttributes attributes;

    @Mock
    private HttpServletRequest request;

    @Test
    void apply_ShouldAddAuthorizationHeader_WhenRequestExists() {
        String expectedToken = "Bearer test-jwt-token";

        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("Authorization")).thenReturn(expectedToken);

        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            interceptor.apply(requestTemplate);

            verify(requestTemplate).header("Authorization", expectedToken);
            verify(requestTemplate).header("X-Internal-Service", "learning-service");
        }
    }

    @Test
    void apply_ShouldAddInternalHeader_WhenAttributesAreNull() {
        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            interceptor.apply(requestTemplate);

            verify(requestTemplate, never()).header(eq("Authorization"), anyString());
            verify(requestTemplate).header("X-Internal-Service", "learning-service");
        }
    }

    @Test
    void apply_ShouldAddInternalHeader_WhenHeaderIsMissing() {
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("Authorization")).thenReturn(null);

        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            interceptor.apply(requestTemplate);

            verify(requestTemplate, never()).header(eq("Authorization"), anyString());
            verify(requestTemplate).header("X-Internal-Service", "learning-service");
        }
    }
}