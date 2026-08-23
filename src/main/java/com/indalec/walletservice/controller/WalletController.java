package com.indalec.walletservice.controller;

import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.service.WalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController //this receives and returns the HTTP requests
@RequestMapping("/wallets") //defines the base route
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public Wallet createWallet(@RequestBody Wallet wallet) {
        return  walletService.createWallet(wallet);
    }
}
