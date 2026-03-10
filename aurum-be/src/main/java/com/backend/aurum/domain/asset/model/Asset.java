package com.backend.aurum.domain.asset.model;

import com.backend.aurum.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    @Column(nullable = false, length = 3)
    private String originalCurrency = "EUR";

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isFavorite = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
