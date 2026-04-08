package com.yurii.zhuravlov.gateway.filter;

import com.yurii.zhuravlov.gateway.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final JwtService jwtService;

    @Override
    public ServerResponse filter(ServerRequest request, @NonNull HandlerFunction<ServerResponse> next) throws Exception {
        String path = request.uri().getPath();

        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            return next.handle(request);
        }

        String authHeader = request.headers().firstHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Incorrect or absent auth token");
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired auth token");
        }

        Long userId = jwtService.extractUserId(token);
        ServerRequest modifiedRequest = ServerRequest.from(request)
                .header("X-User-Id", userId.toString())
                .build();

        return next.handle(modifiedRequest);
    }
}