package com.sharemyrecipe.dto;

import com.sharemyrecipe.domain.Recipe;
import com.sharemyrecipe.domain.RecipeImage;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record RecipeResponse(
        UUID id,
        UUID chefId,
        String chefHandle,
        String chefDisplayName,
        String title,
        String summary,
        String ingredients,
        String steps,
        List<String> labels,
        String status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ImageResponse> images
) {
    public record ImageResponse(UUID id, String originalUrl, String thumbnailUrl, int displayOrder) {}

    public static RecipeResponse from(Recipe recipe) {
        List<ImageResponse> images = recipe.getImages().stream()
                .map(i -> new ImageResponse(i.getId(), i.getOriginalUrl(), i.getThumbnailUrl(), i.getDisplayOrder()))
                .toList();

        return new RecipeResponse(
                recipe.getId(),
                recipe.getChef().getId(),
                recipe.getChef().getHandle(),
                recipe.getChef().getDisplayName(),
                recipe.getTitle(),
                recipe.getSummary(),
                recipe.getIngredients(),
                recipe.getSteps(),
                recipe.getLabels() == null ? List.of() : Arrays.asList(recipe.getLabels()),
                recipe.getStatus().name(),
                recipe.getPublishedAt(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                images
        );
    }
}
