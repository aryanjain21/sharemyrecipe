package com.sharemyrecipe.service;

import com.sharemyrecipe.domain.Recipe;
import com.sharemyrecipe.domain.RecipeImage;
import com.sharemyrecipe.dto.RecipeResponse;
import com.sharemyrecipe.exception.BadRequestException;
import com.sharemyrecipe.exception.NotFoundException;
import com.sharemyrecipe.repository.RecipeImageRepository;
import com.sharemyrecipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private static final int THUMBNAIL_WIDTH  = 400;
    private static final int THUMBNAIL_HEIGHT = 300;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final RecipeRepository recipeRepository;
    private final RecipeImageRepository imageRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public List<RecipeResponse.ImageResponse> uploadImages(UUID recipeId, UUID chefId,
                                                            List<MultipartFile> files) throws IOException {
        Recipe recipe = recipeRepository.findByIdAndChefId(recipeId, chefId)
                .orElseThrow(() -> new NotFoundException("Recipe not found or access denied"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files provided");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        List<RecipeResponse.ImageResponse> results = new ArrayList<>();
        short order = (short) recipe.getImages().size();

        for (MultipartFile file : files) {
            validateImageFile(file);

            String filename    = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            String thumbName   = "thumb_" + filename;

            Path destPath  = uploadPath.resolve(filename);
            Path thumbPath = uploadPath.resolve(thumbName);

            // Save original
            file.transferTo(destPath);

            // Create thumbnail with Thumbnailator
            Thumbnails.of(destPath.toFile())
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .keepAspectRatio(true)
                    .toFile(thumbPath.toFile());

            String baseUrl = "/uploads/";
            RecipeImage image = RecipeImage.builder()
                    .recipe(recipe)
                    .originalUrl(baseUrl + filename)
                    .thumbnailUrl(baseUrl + thumbName)
                    .displayOrder(order++)
                    .build();

            imageRepository.save(image);
            results.add(new RecipeResponse.ImageResponse(
                    image.getId(), image.getOriginalUrl(), image.getThumbnailUrl(), image.getDisplayOrder()));

            log.info("Image uploaded for recipe {}: {}", recipeId, filename);
        }

        return results;
    }

    @Transactional
    public void deleteImage(UUID imageId, UUID chefId) {
        RecipeImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image not found"));
        if (!image.getRecipe().getChef().getId().equals(chefId)) {
            throw new com.sharemyrecipe.exception.ForbiddenException("Access denied");
        }
        imageRepository.delete(image);
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file type. Allowed: jpeg, png, webp");
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "upload";
        // Remove any path traversal characters
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
