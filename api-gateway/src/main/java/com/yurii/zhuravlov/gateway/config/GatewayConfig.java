package com.yurii.zhuravlov.gateway.config;

import com.yurii.zhuravlov.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes(AuthenticationFilter authFilter) {
        return route("auth-service")
                .route(GatewayRequestPredicates.path("/api/auth/**"), HandlerFunctions.http())
                .before(uri("http://localhost:8081"))
                .build()
                .and(GatewayRouterFunctions.route("course-service")
                        .route(GatewayRequestPredicates.path("/api/courses/**"), HandlerFunctions.http())
                        .filter(authFilter)
                        .before(uri("http://localhost:8082"))
                        .build());
    }
}