package com.sharemyrecipe.worker.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Minimal projection of the recipes table used by the worker to update status.
 */
@Entity
@Table(name = "recipes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

    @Id
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ingredients;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String steps;

    @Column(columnDefinition = "TEXT[]")
    private String[] labels;

    @Column(nullable = false, length = 20)
    private String status;

    private Instant publishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
