package com.sharemyrecipe.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipe_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private String originalUrl;

    private String thumbnailUrl;

    @Builder.Default
    private short displayOrder = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant uploadedAt = Instant.now();
}
