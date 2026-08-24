package com.indalec.walletservice.controller;

import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController //this receives and returns the HTTP requests
@RequestMapping("/wallets") //defines the base route
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public Wallet createWallet(@Valid @RequestBody Wallet wallet) {
        return  walletService.createWallet(wallet);
    }

    @GetMapping("/{id}")
    public Wallet getWallet(@PathVariable UUID id) {
        return walletService.getWallet(id);
    }

    @GetMapping("/{id}/balance")
    public BigDecimal getBalance(@PathVariable UUID id) {
        return walletService.getBalance(id);
    }
}
