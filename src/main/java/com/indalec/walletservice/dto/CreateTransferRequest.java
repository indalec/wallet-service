package com.indalec.walletservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

        @NotNull
        String idempotencyKey
) {
}
