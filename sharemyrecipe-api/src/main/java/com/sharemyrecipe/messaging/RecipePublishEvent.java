package com.sharemyrecipe.messaging;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message published to RabbitMQ when a recipe is queued for async processing.
 * The worker consumes this event and updates the recipe status to PUBLISHED.
 */
public record RecipePublishEvent(
        UUID recipeId,
        UUID chefId,
        String title,
        String summary,
        String ingredients,
        String steps,
        List<String> labels,
        Instant requestedAt
) implements Serializable {}
