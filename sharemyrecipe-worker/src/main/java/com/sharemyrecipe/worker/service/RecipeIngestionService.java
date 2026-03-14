package com.sharemyrecipe.worker.service;

import com.sharemyrecipe.worker.domain.Recipe;
import com.sharemyrecipe.worker.messaging.RecipePublishEvent;
import com.sharemyrecipe.worker.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeIngestionService {

    private final RecipeRepository recipeRepository;

    @Transactional
    public void ingest(RecipePublishEvent event) {
        log.info("Ingesting recipe recipeId={} from chef={}", event.recipeId(), event.chefId());

        Recipe recipe = recipeRepository.findById(event.recipeId())
                .orElseThrow(() -> {
                    log.error("Recipe not found in DB: {}", event.recipeId());
                    return new IllegalStateException("Recipe not found: " + event.recipeId());
                });

        // Update fields and transition to PUBLISHED
        recipe.setTitle(event.title());
        recipe.setSummary(event.summary());
        recipe.setIngredients(event.ingredients());
        recipe.setSteps(event.steps());
        recipe.setLabels(event.labels() == null ? null : event.labels().toArray(String[]::new));
        recipe.setStatus("PUBLISHED");
        recipe.setPublishedAt(Instant.now());

        recipeRepository.save(recipe);
        log.info("Recipe {} successfully published at {}", recipe.getId(), recipe.getPublishedAt());
    }
}
