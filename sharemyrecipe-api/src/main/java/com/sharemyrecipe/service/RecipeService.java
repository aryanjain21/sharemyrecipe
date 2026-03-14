package com.sharemyrecipe.service;

import com.sharemyrecipe.domain.*;
import com.sharemyrecipe.dto.*;
import com.sharemyrecipe.exception.*;
import com.sharemyrecipe.messaging.*;
import com.sharemyrecipe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final RecipeEventPublisher eventPublisher;

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    // ── Public feed ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<RecipeResponse> getPublicRecipes(
            String keyword, String chefHandle, UUID chefId,
            Instant from, Instant to, int page, int pageSize) {

        pageSize = clampPageSize(pageSize);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("publishedAt").descending());

        UUID resolvedChefId = chefId;
        if (resolvedChefId == null && chefHandle != null && !chefHandle.isBlank()) {
            resolvedChefId = userRepository.findByHandle(chefHandle)
                    .map(User::getId)
                    .orElseThrow(() -> new NotFoundException("Chef not found: " + chefHandle));
        }

        String likeKeyword = keyword != null ? "%" + keyword.toLowerCase() + "%" : null;

        Page<Recipe> result = recipeRepository.findPublicRecipes(
                resolvedChefId, likeKeyword, from, to, pageable);

        return toPagedResponse(result, page, pageSize);
    }

    @Transactional(readOnly = true)
    public PagedResponse<RecipeResponse> getFeedForFollower(
            UUID followerId, String keyword, Instant from, Instant to,
            int page, int pageSize) {

        pageSize = clampPageSize(pageSize);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("publishedAt").descending());

        List<UUID> followedIds = followRepository.findFollowingByFollowerId(followerId)
                .stream().map(User::getId).toList();

        if (followedIds.isEmpty()) {
            return emptyPage(page, pageSize);
        }

        String likeKeyword = keyword != null ? "%" + keyword.toLowerCase() + "%" : null;
        Page<Recipe> result = recipeRepository.findFollowedChefRecipes(followedIds, likeKeyword, from, to, pageable);
        return toPagedResponse(result, page, pageSize);
    }

    @Transactional(readOnly = true)
    public RecipeResponse getById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + id));
        if (recipe.getStatus() != RecipeStatus.PUBLISHED) {
            throw new NotFoundException("Recipe not found: " + id);
        }
        return RecipeResponse.from(recipe);
    }

    // ── Authoring ─────────────────────────────────────────────────────────────

    @Transactional
    public RecipeResponse createRecipe(UUID chefId, CreateRecipeRequest request) {
        User chef = getChefUser(chefId);

        String[] labels = request.labels() == null ? null
                : request.labels().toArray(String[]::new);

        Recipe recipe = Recipe.builder()
                .chef(chef)
                .title(request.title())
                .summary(request.summary())
                .ingredients(request.ingredients())
                .steps(request.steps())
                .labels(labels)
                .status(RecipeStatus.DRAFT)
                .build();

        recipeRepository.save(recipe);

        if (request.publish()) {
            queueForPublishing(recipe);
        }

        log.info("Recipe created: {} by chef {}", recipe.getId(), chefId);
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public RecipeResponse updateRecipe(UUID chefId, UUID recipeId, UpdateRecipeRequest request, boolean isAdmin) {
        Recipe recipe = findEditableRecipe(chefId, recipeId, isAdmin);

        if (request.title()       != null) recipe.setTitle(request.title());
        if (request.summary()     != null) recipe.setSummary(request.summary());
        if (request.ingredients() != null) recipe.setIngredients(request.ingredients());
        if (request.steps()       != null) recipe.setSteps(request.steps());
        if (request.labels()      != null) recipe.setLabels(request.labels().toArray(String[]::new));

        recipeRepository.save(recipe);
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public RecipeResponse publishRecipe(UUID chefId, UUID recipeId, boolean isAdmin) {
        Recipe recipe = findEditableRecipe(chefId, recipeId, isAdmin);
        queueForPublishing(recipe);
        return RecipeResponse.from(recipe);
    }

    @Transactional
    public void deleteRecipe(UUID chefId, UUID recipeId, boolean isAdmin) {
        Recipe recipe = findEditableRecipe(chefId, recipeId, isAdmin);
        recipeRepository.delete(recipe);
        log.info("Recipe {} deleted by user {}", recipeId, chefId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<RecipeResponse> getMyRecipes(UUID chefId, int page, int pageSize) {
        pageSize = clampPageSize(pageSize);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        // Return all recipes owned by this chef (all statuses)
        Page<Recipe> chefRecipes = recipeRepository.findAllByChefId(chefId, pageable);
        return toPagedResponse(chefRecipes, page, pageSize);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void queueForPublishing(Recipe recipe) {
        recipe.setStatus(RecipeStatus.DRAFT); // stays DRAFT until worker confirms
        recipeRepository.save(recipe);

        RecipePublishEvent event = new RecipePublishEvent(
                recipe.getId(),
                recipe.getChef().getId(),
                recipe.getTitle(),
                recipe.getSummary(),
                recipe.getIngredients(),
                recipe.getSteps(),
                recipe.getLabels() == null ? List.of() : List.of(recipe.getLabels()),
                Instant.now()
        );
        eventPublisher.publishRecipeEvent(event);
        log.info("Recipe {} queued for publishing", recipe.getId());
    }

    private Recipe findEditableRecipe(UUID userId, UUID recipeId, boolean isAdmin) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe not found: " + recipeId));
        if (!isAdmin && !recipe.getChef().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this recipe");
        }
        return recipe;
    }

    private User getChefUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() == Role.USER) {
            throw new ForbiddenException("Only chefs can author recipes. Upgrade your account to CHEF role.");
        }
        return user;
    }

    private int clampPageSize(int pageSize) {
        if (pageSize <= 0) return defaultPageSize;
        return Math.min(pageSize, maxPageSize);
    }

    private PagedResponse<RecipeResponse> toPagedResponse(Page<Recipe> page, int pageNum, int pageSize) {
        List<RecipeResponse> content = page.getContent().stream().map(RecipeResponse::from).toList();
        var meta = new PagedResponse.PaginationMeta(
                pageNum, pageSize,
                page.getTotalElements(), page.getTotalPages(),
                page.hasNext(), page.hasPrevious()
        );
        return new PagedResponse<>(content, meta);
    }

    private PagedResponse<RecipeResponse> emptyPage(int page, int pageSize) {
        var meta = new PagedResponse.PaginationMeta(page, pageSize, 0, 0, false, false);
        return new PagedResponse<>(List.of(), meta);
    }
}
