package com.yurii.zhuravlov.learningservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String COURSE_QUEUE = "learning.course.updates";
    public static final String COURSE_EXCHANGE = "course.exchange";

    @Bean
    public Queue courseQueue() { return new Queue(COURSE_QUEUE); }

    @Bean
    public FanoutExchange courseExchange() { return new FanoutExchange(COURSE_EXCHANGE); }

    @Bean
    public Binding binding(Queue courseQueue, FanoutExchange courseExchange) {
        return BindingBuilder.bind(courseQueue).to(courseExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}