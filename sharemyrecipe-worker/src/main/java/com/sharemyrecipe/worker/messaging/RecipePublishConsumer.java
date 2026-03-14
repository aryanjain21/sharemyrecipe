package com.sharemyrecipe.worker.messaging;

import com.sharemyrecipe.worker.service.RecipeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipePublishConsumer {

    private final RecipeIngestionService ingestionService;

    @RabbitListener(queues = RabbitMQConfig.RECIPE_QUEUE)
    public void consume(RecipePublishEvent event) {
        log.info("Received publish event for recipeId={}", event.recipeId());
        try {
            ingestionService.ingest(event);
        } catch (Exception ex) {
            log.error("Failed to process recipe publish event for recipeId={}: {}",
                    event.recipeId(), ex.getMessage(), ex);
            // Re-throwing causes RabbitMQ retry + eventual DLQ routing
            throw ex;
        }
    }
}
