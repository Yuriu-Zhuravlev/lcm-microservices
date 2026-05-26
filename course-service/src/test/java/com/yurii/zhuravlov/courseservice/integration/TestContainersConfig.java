package com.yurii.zhuravlov.courseservice.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

public abstract class TestContainersConfig {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;
    static final RabbitMQContainer RABBITMQ;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("lms_db")
                .withUsername("user")
                .withPassword("password");

        REDIS = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);

        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-alpine");

        POSTGRES.start();
        REDIS.start();
        RABBITMQ.start();
    }
}