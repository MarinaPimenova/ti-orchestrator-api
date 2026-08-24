package com.wk.ti.rabbit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("removal")
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "ti.import";
    // queues
    public static final String QUEUE_IMPORT_REQ = "import-worker.import";
    public static final String QUEUE_IMPORT_COMPLETED = "import-worker.completed";
    public static final String QUEUE_IMPORT_FAIL = "import-worker.fail";

    // routing
    public static final String RK_IMPORT_REQ = "import.requested";
    public static final String RK_IMPORT_COMPLETED = "import.completed";
    public static final String RK_IMPORT_FAILED = "import.failed";

    @Bean
    public TopicExchange importExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue importRequestQueue() {
        return QueueBuilder.durable(QUEUE_IMPORT_REQ).build();
    }

    @Bean
    public Queue importCompletedQueue() {
        return QueueBuilder.durable(QUEUE_IMPORT_COMPLETED).build();
    }

    @Bean
    public Queue importFailQueue() {
        return QueueBuilder.durable(QUEUE_IMPORT_FAIL).build();
    }

    @Bean
    public Binding bindImportRequest(Queue importRequestQueue, TopicExchange importExchange) {
        return BindingBuilder.bind(importRequestQueue).to(importExchange).with(RK_IMPORT_REQ);
    }

    @Bean
    public Binding bindImportCompleted(Queue importCompletedQueue, TopicExchange importExchange) {
        return BindingBuilder.bind(importCompletedQueue).to(importExchange).with(RK_IMPORT_COMPLETED);
    }

    @Bean
    public Binding bindImportFail(Queue importFailQueue, TopicExchange importExchange) {
        return BindingBuilder.bind(importFailQueue).to(importExchange).with(RK_IMPORT_FAILED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
