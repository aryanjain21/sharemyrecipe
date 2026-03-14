package com.sharemyrecipe.worker.repository;

import com.sharemyrecipe.worker.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {}
