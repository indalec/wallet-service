package com.indalec.walletservice.service;

import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.repository.WalletRepository;
import org.springframework.stereotype.Service;

//This class contains the business logic
@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createWallet(Wallet wallet) {
        return walletRepository.save(wallet);
    }
}