package com.indalec.walletservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;


import java.math.BigDecimal;
import java.util.UUID;


@Entity //This represents a table of the database
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; //UUID generates a unique identifier, better in distributed systems,  instead of the simple sequence of a Long


    @NotBlank
    @Size(max = 100)
    private String ownerName;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;


    //We will use BigDecimal instead of Double to solve precision issues
    @NotNull
    @PositiveOrZero
    private BigDecimal balance;



//protected instead of public for encapsulation, to avoid new empty wallets
    protected Wallet() {
    }

    public Wallet(String ownerName, String currency, BigDecimal balance) {
        this.ownerName = ownerName;
        this.currency = currency;
        this.balance = balance;
    }





    public UUID getId() {
        return id;
    }


    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }


}
