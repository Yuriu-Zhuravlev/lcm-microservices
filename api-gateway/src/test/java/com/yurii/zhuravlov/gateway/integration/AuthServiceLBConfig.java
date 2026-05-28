// AuthServiceLBConfig.java
package com.yurii.zhuravlov.gateway.integration;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import java.util.List;

public class AuthServiceLBConfig {
    @Bean
    public ServiceInstanceListSupplier supplier() {
        return new ServiceInstanceListSupplier() {
            @Override public @NonNull String getServiceId() { return "auth-service"; }
            @Override public Flux<List<ServiceInstance>> get() {
                return Flux.just(List.of(new DefaultServiceInstance(
                        "auth-1", "auth-service", "localhost",
                        BaseGatewayIntegrationTest.authServiceMock.getPort(), false)));
            }
        };
    }
}