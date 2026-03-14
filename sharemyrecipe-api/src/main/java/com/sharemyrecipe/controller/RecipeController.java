package com.sharemyrecipe.controller;

import com.sharemyrecipe.dto.*;
import com.sharemyrecipe.security.AuthenticatedUserResolver;
import com.sharemyrecipe.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Recipes", description = "Recipe browsing and authoring")
public class RecipeController {

    private final RecipeService recipeService;
    private final AuthenticatedUserResolver userResolver;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/recipes")
    @Operation(summary = "Browse all published recipes (public)")
    public ResponseEntity<PagedResponse<RecipeResponse>> listPublicRecipes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID chefId,
            @RequestParam(required = false) String chefHandle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant publishedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant publishedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        return ResponseEntity.ok(
                recipeService.getPublicRecipes(q, chefHandle, chefId, publishedFrom, publishedTo, page, pageSize)
        );
    }

    @GetMapping("/recipes/{id}")
    @Operation(summary = "Get a single published recipe by ID")
    public ResponseEntity<RecipeResponse> getRecipe(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.getById(id));
    }

    // ── Authenticated: Feed from followed chefs ───────────────────────────────

    @GetMapping("/feed")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get recipe feed from all followed chefs")
    public ResponseEntity<PagedResponse<RecipeResponse>> getFeed(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant publishedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant publishedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        UUID currentUserId = userResolver.getCurrentUserId();
        return ResponseEntity.ok(
                recipeService.getFeedForFollower(currentUserId, q, publishedFrom, publishedTo, page, pageSize)
        );
    }

    // ── Chef authoring ────────────────────────────────────────────────────────

    @PostMapping("/chef/recipes")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new recipe (CHEF/ADMIN only)")
    public ResponseEntity<RecipeResponse> createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        UUID chefId = userResolver.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recipeService.createRecipe(chefId, request));
    }

    @PatchMapping("/chef/recipes/{id}")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an existing recipe")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @RequestBody UpdateRecipeRequest request) {

        UUID userId  = userResolver.getCurrentUserId();
        boolean admin = isAdmin();
        return ResponseEntity.ok(recipeService.updateRecipe(userId, id, request, admin));
    }

    @PostMapping("/chef/recipes/{id}/publish")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Queue a draft recipe for publishing")
    public ResponseEntity<RecipeResponse> publishRecipe(@PathVariable UUID id) {
        UUID userId = userResolver.getCurrentUserId();
        boolean admin = isAdmin();
        return ResponseEntity.ok(recipeService.publishRecipe(userId, id, admin));
    }

    @DeleteMapping("/chef/recipes/{id}")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a recipe")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        UUID userId = userResolver.getCurrentUserId();
        boolean admin = isAdmin();
        recipeService.deleteRecipe(userId, id, admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chef/recipes")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List all own recipes (drafts + published)")
    public ResponseEntity<PagedResponse<RecipeResponse>> getMyRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        UUID chefId = userResolver.getCurrentUserId();
        return ResponseEntity.ok(recipeService.getMyRecipes(chefId, page, pageSize));
    }

    private boolean isAdmin() {
        return userResolver.getCurrentUser().getRole().name().equals("ADMIN");
    }
}
