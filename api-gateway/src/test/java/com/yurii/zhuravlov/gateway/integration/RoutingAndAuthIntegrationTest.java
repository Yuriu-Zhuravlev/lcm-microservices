package com.yurii.zhuravlov.gateway.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingAndAuthIntegrationTest extends BaseGatewayIntegrationTest {

    @Test
    void courses_ShouldReturn401_WhenNoAuthHeader() throws InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/courses/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        RecordedRequest unwanted = courseServiceMock.takeRequest(300, TimeUnit.MILLISECONDS);
        assertThat(unwanted).as("downstream didn't get request").isNull();
    }

    @Test
    void learning_ShouldReturn401_WhenNoAuthHeader() throws InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/learning/progress", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        RecordedRequest unwanted = learningServiceMock.takeRequest(300, TimeUnit.MILLISECONDS);
        assertThat(unwanted).as("downstream didn't get request").isNull();
    }

    @Test
    void auth_ShouldReturn401_WhenNoAuthHeader() throws InterruptedException {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/auth/users/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        RecordedRequest unwanted = authServiceMock.takeRequest(300, TimeUnit.MILLISECONDS);
        assertThat(unwanted).as("downstream didn't get request").isNull();
    }

    @Test
    void courses_ShouldReturn401_WhenAuthHeaderHasWrongScheme() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token some-token-value");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        RecordedRequest unwanted = courseServiceMock.takeRequest(300, TimeUnit.MILLISECONDS);
        assertThat(unwanted).as("downstream didn't get request").isNull();
    }

    @Test
    void courses_ShouldReturn401_WhenTokenIsInvalid() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("totally.invalid.token");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        RecordedRequest unwanted = courseServiceMock.takeRequest(300, TimeUnit.MILLISECONDS);
        assertThat(unwanted).as("downstream didn't get request").isNull();
    }

    @Test
    void courses_ShouldReturn401_WhenTokenIsExpired() throws InterruptedException {
        String expiredToken = generateExpiredToken("user@test.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        RecordedRequest unwanted = courseServiceMock.takeRequest(300, TimeUnit.MILLISECONDS);
        assertThat(unwanted).as("downstream didn't get request").isNull();
    }

    @Test
    void courses_ShouldProxyRequest_WhenValidToken() throws InterruptedException {
        courseServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"id\": 42, \"title\": \"Spring Boot\"}"));

        String token = generateValidToken("user@test.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/42", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Spring Boot");

        RecordedRequest recorded = courseServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/courses/42");
        assertThat(recorded.getMethod()).isEqualTo("GET");
    }

    @Test
    void learning_ShouldProxyRequest_WhenValidToken() throws InterruptedException {
        learningServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"progress\": 75}"));

        String token = generateValidToken("user@test.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/learning/progress", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("75");

        RecordedRequest recorded = learningServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/learning/progress");
    }

    @Test
    void courses_ShouldPreserveDownstreamStatusCode_WhenProxying() {
        courseServiceMock.enqueue(new MockResponse().setResponseCode(404));

        String token = generateValidToken("user@test.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/999", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void authLogin_ShouldProxy_WithoutAnyToken() throws InterruptedException {
        authServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"token\": \"jwt-here\"}"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"email\":\"u@u.com\",\"password\":\"pass\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jwt-here");

        RecordedRequest recorded = authServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/auth/login");
        assertThat(recorded.getMethod()).isEqualTo("POST");
    }

    @Test
    void authRegister_ShouldProxy_WithoutAnyToken() throws InterruptedException {
        authServiceMock.enqueue(new MockResponse().setResponseCode(201));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"email\":\"new@u.com\",\"password\":\"pass\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/register", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        RecordedRequest recorded = authServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/auth/register");
    }

    @Test
    void gateway_ShouldStrip_XInternalServiceHeader_ForCourseRequests() throws InterruptedException {
        courseServiceMock.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        String token = generateValidToken("user@test.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Internal-Service", "malicious-override");

        restTemplate.exchange("/api/courses/1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        RecordedRequest recorded = courseServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-Internal-Service")).isNull();
    }

    @Test
    void gateway_ShouldStrip_XInternalServiceHeader_ForLearningRequests() throws InterruptedException {
        learningServiceMock.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        String token = generateValidToken("user@test.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Internal-Service", "fake-service");

        restTemplate.exchange("/api/learning/1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        RecordedRequest recorded = learningServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-Internal-Service")).isNull();
    }
}