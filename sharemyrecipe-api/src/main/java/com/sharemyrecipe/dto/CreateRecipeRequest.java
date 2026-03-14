package com.sharemyrecipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRecipeRequest(
        @NotBlank @Size(max = 255) String title,
        String summary,
        @NotBlank String ingredients,
        @NotBlank String steps,
        List<String> labels,
        boolean publish   // true → immediately publish
) {}
