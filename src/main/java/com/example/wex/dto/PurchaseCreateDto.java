package com.example.wex.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PurchaseCreateDto(

        @NotBlank
        @Size(max = 50)
        String description,

        @NotNull
        LocalDate transactionDate,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        Double amountUsd

) {}
