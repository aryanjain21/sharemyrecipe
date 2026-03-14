package com.sharemyrecipe.dto;

import java.util.List;

public record UpdateRecipeRequest(
        String title,
        String summary,
        String ingredients,
        String steps,
        List<String> labels
) {}
