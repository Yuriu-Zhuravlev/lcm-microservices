package com.yurii.zhuravlov.gateway.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerIntegrationTest extends BaseGatewayIntegrationTest {

    @Test
    void swaggerUiHtml_ShouldBeAccessible_WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode().value()).isIn(200, 302);
    }

    @Test
    void swaggerUiIndex_ShouldBeAccessible_WithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsIgnoringCase("swagger");
    }

    @Test
    void authApiDocs_ShouldProxy_WithoutToken() throws InterruptedException {
        authServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Auth Service\"}}"));

        ResponseEntity<String> response = restTemplate.getForEntity("/api/auth/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("openapi");

        RecordedRequest recorded = authServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/auth/v3/api-docs");
    }

    @Test
    void courseApiDocs_ShouldProxy_WithoutToken() throws InterruptedException {
        courseServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Course Service\"}}"));

        ResponseEntity<String> response = restTemplate.getForEntity("/api/courses/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("openapi");

        RecordedRequest recorded = courseServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/courses/v3/api-docs");
    }

    @Test
    void learningApiDocs_ShouldProxy_WithoutToken() throws InterruptedException {
        learningServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Learning Service\"}}"));

        ResponseEntity<String> response = restTemplate.getForEntity("/api/learning/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("openapi");

        RecordedRequest recorded = learningServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/learning/v3/api-docs");
    }

    @Test
    void courseApiDocs_ShouldNotRequireToken_EvenWithAuthFilterOnCourseRoute() throws InterruptedException {
        courseServiceMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"openapi\":\"3.0.1\"}"));

        ResponseEntity<String> response = restTemplate.getForEntity("/api/courses/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        RecordedRequest recorded = courseServiceMock.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
    }
}