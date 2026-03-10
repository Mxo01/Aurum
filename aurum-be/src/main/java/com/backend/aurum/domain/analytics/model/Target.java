package com.backend.aurum.domain.analytics.model;

import com.backend.aurum.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "targets")
@Getter
@Setter
@NoArgsConstructor
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    private LocalDate deadline;

    @Column(nullable = false)
    private Boolean isCompleted = false;
}
