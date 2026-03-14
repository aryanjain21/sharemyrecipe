package com.sharemyrecipe.repository;

import com.sharemyrecipe.domain.RecipeImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeImageRepository extends JpaRepository<RecipeImage, UUID> {
    List<RecipeImage> findByRecipeIdOrderByDisplayOrderAsc(UUID recipeId);
}
