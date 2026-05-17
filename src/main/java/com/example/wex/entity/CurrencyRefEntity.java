package com.example.wex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "treasury_currency_refs",
        indexes = {
@Index(
        name = "idx_ccd",
        columnList = "country_currency_desc",
        unique = true
        )
        }
    )

public class CurrencyRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(
            name = "country_currency_desc",
            nullable = false,
            length = 200,
            unique = true
    )
    private String countryCurrencyDesc;
    public CurrencyRefEntity() {}

    @Setter
    @Column(length = 100)
    private String country;

    @Setter
    @Column(length = 100)
    private String currency;

    @Setter
    @Column(nullable = false)
    private Instant lastUpdated;
}