package com.sharemyrecipe.controller;

import com.sharemyrecipe.dto.RecipeResponse;
import com.sharemyrecipe.security.AuthenticatedUserResolver;
import com.sharemyrecipe.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef/recipes/{recipeId}/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Recipe image upload and management")
@SecurityRequirement(name = "bearerAuth")
public class ImageController {

    private final ImageService imageService;
    private final AuthenticatedUserResolver userResolver;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @Operation(summary = "Upload one or more images for a recipe (auto-resized to thumbnail)")
    public ResponseEntity<List<RecipeResponse.ImageResponse>> uploadImages(
            @PathVariable UUID recipeId,
            @RequestPart("files") List<MultipartFile> files) throws IOException {

        UUID chefId = userResolver.getCurrentUserId();
        List<RecipeResponse.ImageResponse> result = imageService.uploadImages(recipeId, chefId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('CHEF', 'ADMIN')")
    @Operation(summary = "Delete a recipe image")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID recipeId,
            @PathVariable UUID imageId) {

        UUID chefId = userResolver.getCurrentUserId();
        imageService.deleteImage(imageId, chefId);
        return ResponseEntity.noContent().build();
    }
}
