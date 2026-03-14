package com.sharemyrecipe.repository;

import com.sharemyrecipe.domain.Recipe;
import com.sharemyrecipe.domain.RecipeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {

    Page<Recipe> findByChefIdAndStatus(UUID chefId, RecipeStatus status, Pageable pageable);

    Page<Recipe> findAllByChefId(UUID chefId, Pageable pageable);

    Optional<Recipe> findByIdAndChefId(UUID id, UUID chefId);

    @Query("""
            SELECT r FROM Recipe r
            WHERE r.chef.id IN :chefIds
              AND r.status = 'PUBLISHED'
              AND (:keyword IS NULL OR
                   (LOWER(r.title) LIKE :keyword OR
                    LOWER(r.summary) LIKE :keyword OR
                    LOWER(r.ingredients) LIKE :keyword OR
                    LOWER(r.steps) LIKE :keyword))
              AND (:from IS NULL OR r.publishedAt >= :from)
              AND (:to   IS NULL OR r.publishedAt <= :to)
            """)
    Page<Recipe> findFollowedChefRecipes(
            @Param("chefIds") List<UUID> chefIds,
            @Param("keyword") String keyword,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM Recipe r
            WHERE r.status = 'PUBLISHED'
              AND (:chefId IS NULL OR r.chef.id = :chefId)
              AND (:keyword IS NULL OR
                   (LOWER(r.title) LIKE :keyword OR
                    LOWER(r.summary) LIKE :keyword OR
                    LOWER(r.ingredients) LIKE :keyword OR
                    LOWER(r.steps) LIKE :keyword))
              AND (:from IS NULL OR r.publishedAt >= :from)
              AND (:to   IS NULL OR r.publishedAt <= :to)
            """)
    Page<Recipe> findPublicRecipes(
            @Param("chefId") UUID chefId,
            @Param("keyword") String keyword,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
