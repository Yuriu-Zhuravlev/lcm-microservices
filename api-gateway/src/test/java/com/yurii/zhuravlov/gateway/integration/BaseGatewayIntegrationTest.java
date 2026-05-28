package com.yurii.zhuravlov.gateway.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockWebServer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@LoadBalancerClients({
        @LoadBalancerClient(name = "auth-service",     configuration = AuthServiceLBConfig.class),
        @LoadBalancerClient(name = "course-service",   configuration = CourseServiceLBConfig.class),
        @LoadBalancerClient(name = "learning-service", configuration = LearningServiceLBConfig.class)
})
public abstract class BaseGatewayIntegrationTest {

    protected static final MockWebServer authServiceMock = new MockWebServer();
    protected static final MockWebServer courseServiceMock = new MockWebServer();
    protected static final MockWebServer learningServiceMock = new MockWebServer();

    static {
        try {
            authServiceMock.start();
            courseServiceMock.start();
            learningServiceMock.start();
        } catch (IOException e) {
            throw new RuntimeException("Не вдалось запустити MockWebServer", e);
        }
    }



    @LocalServerPort
    private int port;

    protected RestTemplate restTemplate;

    @BeforeEach
    void setUpRestTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .requestFactory(() -> factory)
                .build();

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(@NonNull ClientHttpResponse response) {
                return false;
            }
        });
    }

    @Value("${jwt.secret}")
    private String jwtSecret;

    @AfterEach
    void drainMockQueues() throws InterruptedException {
        drainServer(authServiceMock);
        drainServer(courseServiceMock);
        drainServer(learningServiceMock);
    }

    private void drainServer(MockWebServer server) throws InterruptedException {
        server.takeRequest(50, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    protected String generateValidToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    protected String generateExpiredToken(String username) {
        javax.crypto.SecretKey key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret));
        return io.jsonwebtoken.Jwts.builder()
                .subject(username)
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new java.util.Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key)
                .compact();
    }
}