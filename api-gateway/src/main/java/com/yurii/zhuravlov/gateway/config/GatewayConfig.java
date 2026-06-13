package com.yurii.zhuravlov.gateway.config;

import com.yurii.zhuravlov.gateway.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.removeRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions.circuitBreaker;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayConfig {

    private static final URI AUTH_FALLBACK     = URI.create("forward:/fallback/auth-service");
    private static final URI COURSE_FALLBACK   = URI.create("forward:/fallback/course-service");
    private static final URI LEARNING_FALLBACK = URI.create("forward:/fallback/learning-service");

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes(AuthenticationFilter authFilter) {
        return route("auth-api-docs")
                .route(path("/api/auth/v3/api-docs"), http())
                .filter(lb("auth-service"))
                .build()
                .and(route("course-api-docs")
                        .route(path("/api/courses/v3/api-docs"), http())
                        .filter(lb("course-service"))
                        .build())
                .and(route("learning-api-docs")
                        .route(path("/api/learning/v3/api-docs"), http())
                        .filter(lb("learning-service"))
                        .build())
                .and(route("auth-service")
                        .route(path("/api/auth/**"), http())
                        .before(removeRequestHeader("X-Internal-Service"))
                        .filter(authFilter)
                        .filter(circuitBreaker("auth-service", AUTH_FALLBACK))
                        .filter(lb("auth-service"))
                        .build())
                .and(route("course-service")
                        .route(path("/api/courses/**"), http())
                        .before(removeRequestHeader("X-Internal-Service"))
                        .filter(authFilter)
                        .filter(circuitBreaker("course-service", COURSE_FALLBACK))
                        .filter(lb("course-service"))
                        .build())
                .and(route("learning-service")
                        .route(path("/api/learning/**"), http())
                        .before(removeRequestHeader("X-Internal-Service"))
                        .filter(authFilter)
                        .filter(circuitBreaker("learning-service", LEARNING_FALLBACK))
                        .filter(lb("learning-service"))
                        .build());
    }
}