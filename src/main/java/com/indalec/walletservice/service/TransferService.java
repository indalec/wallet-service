package com.indalec.walletservice.service;

import com.indalec.walletservice.exception.TransferException;
import com.indalec.walletservice.exception.WalletNotFoundException;
import com.indalec.walletservice.model.Transfer;
import com.indalec.walletservice.model.TransferStatus;
import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.repository.TransferRepository;
import com.indalec.walletservice.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;

    public TransferService(
            WalletRepository walletRepository,
            TransferRepository transferRepository
    ) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
    }

    //With Transactional the transfer will either complete entirely or have no effect at all.
    @Transactional
    public Transfer transfer(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {

        Transfer existingTransfer =
                transferRepository.findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existingTransfer != null) {
            return existingTransfer;
        }

        Wallet source = walletRepository.findById(sourceWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        Wallet destination = walletRepository.findById(destinationWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new TransferException("A wallet cannot transfer money to itself");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferException("Transfer amount must be greater than zero");
        }

        if (!source.getCurrency().equals(currency)
                || !destination.getCurrency().equals(currency)) {
            throw new TransferException("Currency mismatch");
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new TransferException("Insufficient funds");
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        Transfer transfer = new Transfer(
                source,
                destination,
                amount,
                currency,
                LocalDateTime.now(),
                TransferStatus.COMPLETED,
                idempotencyKey
        );

        return transferRepository.save(transfer);
    }
}