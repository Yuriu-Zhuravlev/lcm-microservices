// LearningServiceLBConfig.java
package com.yurii.zhuravlov.gateway.integration;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import java.util.List;

public class LearningServiceLBConfig {
    @Bean
    public ServiceInstanceListSupplier supplier() {
        return new ServiceInstanceListSupplier() {
            @Override public @NonNull String getServiceId() { return "learning-service"; }
            @Override public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(new DefaultServiceInstance(
                        "learning-1", "learning-service", "localhost",
                        BaseGatewayIntegrationTest.learningServiceMock.getPort(), false)));
            }
        };
    }
}