package com.indalec.walletservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


import java.math.BigDecimal;
import java.util.UUID;


@Entity //This represents a table of the database
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; //UUID generates a unique identifier, better in distributed systems,  instead of the simple sequence of a Long

    private String ownerName;

    private String currency;


    //We will use BigDecimal instead of Double to solve precision issues
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
