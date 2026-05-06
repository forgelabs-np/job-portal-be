package com.jobportal.v1.entity.master;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "countries")
@Data
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(length = 10)
    private String phoneCode;

    @Column(length = 10)
    private String currencyCode;

    @Column(length = 50)
    private String currencyName;

    @Column(length = 10)
    private String currencySymbol;

    @Column(length = 100)
    private String capital;

    @Column(length = 50)
    private String region;

    @Column(length = 50)
    private String subregion;

    @Column(name = "is_enabled")
    private Boolean isEnabled = false; // DEFAULT FALSE - admin enables

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}