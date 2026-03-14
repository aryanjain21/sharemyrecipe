package com.sharemyrecipe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email String email,

        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-z0-9_-]+$", message = "Handle may only contain lowercase letters, digits, underscores and hyphens")
        String handle,

        @NotBlank @Size(min = 2, max = 100) String displayName,

        @NotBlank @Size(min = 8, max = 128) String password,

        String role   // optional – "CHEF" or "USER"; defaults to USER
) {}
