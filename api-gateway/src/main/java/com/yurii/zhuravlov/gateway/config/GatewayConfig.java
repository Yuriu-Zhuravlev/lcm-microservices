package com.yurii.zhuravlov.gateway.config;

import com.yurii.zhuravlov.gateway.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayConfig {

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
                        .filter(lb("auth-service"))
                        .build())
                .and(route("course-service")
                        .route(path("/api/courses/**"), http())
                        .filter(authFilter)
                        .filter(lb("course-service"))
                        .build())
                .and(route("learning-service")
                        .route(path("/api/learning/**"), http())
                        .filter(authFilter)
                        .filter(lb("learning-service"))
                        .build());
    }
}