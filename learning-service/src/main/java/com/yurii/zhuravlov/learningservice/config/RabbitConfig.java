package com.yurii.zhuravlov.learningservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String COURSE_QUEUE = "learning.course.updates";
    public static final String COURSE_EXCHANGE = "course.exchange";
    public static final String COURSE_DLQ = "learning.course.updates.dlq";
    public static final String COURSE_DLX = "learning.course.updates.dlx";

    @Bean
    public Queue courseQueue() {
        return QueueBuilder.durable(COURSE_QUEUE)
                .withArgument("x-dead-letter-exchange", COURSE_DLX)
                .build();
    }

    @Bean
    public FanoutExchange courseExchange() { return new FanoutExchange(COURSE_EXCHANGE); }

    @Bean
    public Binding binding(Queue courseQueue, FanoutExchange courseExchange) {
        return BindingBuilder.bind(courseQueue).to(courseExchange);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(COURSE_DLQ).build();
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(COURSE_DLX);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}