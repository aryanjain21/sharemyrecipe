package com.sharemyrecipe.controller;

import com.sharemyrecipe.dto.UserResponse;
import com.sharemyrecipe.security.AuthenticatedUserResolver;
import com.sharemyrecipe.service.ChefService;
import com.sharemyrecipe.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Chefs & Following", description = "Chef profiles and follower/following management")
public class ChefController {

    private final ChefService chefService;
    private final FollowService followService;
    private final AuthenticatedUserResolver userResolver;

    // ── Public profiles ───────────────────────────────────────────────────────

    @GetMapping("/chefs/{id}")
    @Operation(summary = "Get chef profile by ID")
    public ResponseEntity<UserResponse> getChefById(@PathVariable UUID id) {
        return ResponseEntity.ok(chefService.getChefProfile(id));
    }

    @GetMapping("/chefs/handle/{handle}")
    @Operation(summary = "Get chef profile by handle")
    public ResponseEntity<UserResponse> getChefByHandle(@PathVariable String handle) {
        return ResponseEntity.ok(chefService.getChefProfileByHandle(handle));
    }

    @GetMapping("/chefs/{id}/stats")
    @Operation(summary = "Get follower/following stats for a chef")
    public ResponseEntity<ChefService.ChefStatsResponse> getStats(@PathVariable UUID id) {
        return ResponseEntity.ok(chefService.getStats(id));
    }

    @GetMapping("/chefs/{id}/followers")
    @Operation(summary = "List followers of a chef")
    public ResponseEntity<List<UserResponse>> getFollowers(@PathVariable UUID id) {
        return ResponseEntity.ok(followService.getFollowers(id));
    }

    @GetMapping("/chefs/{id}/following")
    @Operation(summary = "List chefs that this user follows")
    public ResponseEntity<List<UserResponse>> getFollowing(@PathVariable UUID id) {
        return ResponseEntity.ok(followService.getFollowing(id));
    }

    // ── Authenticated follow actions ──────────────────────────────────────────

    @PostMapping("/chefs/{targetId}/follow")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Follow a chef")
    public ResponseEntity<Void> follow(@PathVariable UUID targetId) {
        followService.follow(userResolver.getCurrentUserId(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/chefs/{targetId}/follow")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unfollow a chef")
    public ResponseEntity<Void> unfollow(@PathVariable UUID targetId) {
        followService.unfollow(userResolver.getCurrentUserId(), targetId);
        return ResponseEntity.noContent().build();
    }
}
