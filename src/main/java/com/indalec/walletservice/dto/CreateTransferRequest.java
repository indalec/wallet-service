package com.indalec.walletservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(

        @NotNull
        UUID sourceWalletId,

        @NotNull
        UUID destinationWalletId,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        String currency,

        @NotBlank //the value has to contain actual text
        @Size(max = 100)
        String idempotencyKey
) {
}
