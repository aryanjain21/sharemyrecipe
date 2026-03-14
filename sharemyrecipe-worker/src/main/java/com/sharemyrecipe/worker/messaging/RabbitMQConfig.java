package com.sharemyrecipe.worker.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RECIPE_EXCHANGE    = "recipe.exchange";
    public static final String RECIPE_QUEUE       = "recipe.publish.queue";
    public static final String RECIPE_ROUTING_KEY = "recipe.publish";
    public static final String RECIPE_DLQ         = "recipe.publish.dlq";
    public static final String RECIPE_DL_EXCHANGE  = "recipe.dlx";

    @Bean
    public DirectExchange recipeExchange() {
        return ExchangeBuilder.directExchange(RECIPE_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(RECIPE_DL_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue recipePublishQueue() {
        return QueueBuilder.durable(RECIPE_QUEUE)
                .withArgument("x-dead-letter-exchange", RECIPE_DL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RECIPE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RECIPE_DLQ).build();
    }

    @Bean
    public Binding recipeBinding(Queue recipePublishQueue, DirectExchange recipeExchange) {
        return BindingBuilder.bind(recipePublishQueue).to(recipeExchange).with(RECIPE_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(RECIPE_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
