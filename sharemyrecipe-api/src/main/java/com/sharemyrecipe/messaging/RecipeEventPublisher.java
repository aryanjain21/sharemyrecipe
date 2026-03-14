package com.sharemyrecipe.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishRecipeEvent(RecipePublishEvent event) {
        log.info("Publishing recipe event for recipeId={}", event.recipeId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RECIPE_EXCHANGE,
                RabbitMQConfig.RECIPE_ROUTING_KEY,
                event
        );
    }
}
