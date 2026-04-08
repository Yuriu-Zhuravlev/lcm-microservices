package com.yurii.zhuravlov.gateway.config;

import com.yurii.zhuravlov.gateway.filter.AuthenticationFilter;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes(AuthenticationFilter authFilter) {
        return route("auth-service")
                .route(GatewayRequestPredicates.path("/api/auth/**"), HandlerFunctions.http())
                .filter(lb("auth-service"))
                .build()
                .and(GatewayRouterFunctions.route("course-service")
                        .route(GatewayRequestPredicates.path("/api/courses/**"), HandlerFunctions.http())
                        .filter(authFilter)
                        .filter(lb("course-service"))
                        .build());
    }
}