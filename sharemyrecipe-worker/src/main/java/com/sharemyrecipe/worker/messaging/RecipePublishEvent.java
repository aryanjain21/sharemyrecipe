package com.sharemyrecipe.worker.messaging;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors the RecipePublishEvent from the API module.
 * Deserialized from the RabbitMQ message payload.
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
