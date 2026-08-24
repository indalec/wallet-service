package com.indalec.walletservice.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "source_wallet_id", nullable = false)
    private Wallet sourceWallet;

    @ManyToOne
    @JoinColumn(name = "destination_wallet_id", nullable = false)
    private Wallet destinationWallet;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    protected Transfer() {
    }

    public Transfer(
            Wallet sourceWallet,
            Wallet destinationWallet,
            BigDecimal amount,
            String currency,
            LocalDateTime createdAt,
            TransferStatus status,
            String idempotencyKey

    ) {
        this.sourceWallet = sourceWallet;
        this.destinationWallet = destinationWallet;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public Wallet getSourceWallet() {
        return sourceWallet;
    }

    public Wallet getDestinationWallet() {
        return destinationWallet;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TransferStatus getStatus() {
        return status;
    }
}