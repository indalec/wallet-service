package com.indalec.walletservice.service;

import com.indalec.walletservice.exception.TransferException;
import com.indalec.walletservice.exception.WalletNotFoundException;
import com.indalec.walletservice.model.Transfer;
import com.indalec.walletservice.model.TransferStatus;
import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.repository.TransferRepository;
import com.indalec.walletservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransferTransactionService {

    private static final Logger log =
            LoggerFactory.getLogger(TransferTransactionService.class);

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;

    public TransferTransactionService(
            WalletRepository walletRepository,
            TransferRepository transferRepository
    ) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
    }

    // With Transactional the transfer will either complete entirely or have no effect at all.
    @Transactional
    public Transfer executeTransfer(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {

        Wallet source = walletRepository.findByIdForUpdate(sourceWalletId)
                .orElseThrow(() -> {
                    log.warn("Transfer failed: source wallet not found. walletId={}", sourceWalletId);
                    return new WalletNotFoundException("Wallet not found");
                });

        Wallet destination = walletRepository.findByIdForUpdate(destinationWalletId)
                .orElseThrow(() -> {
                    log.warn("Transfer failed: destination wallet not found. walletId={}", destinationWalletId);
                    return new WalletNotFoundException("Wallet not found");
                });

        if (sourceWalletId.equals(destinationWalletId)) {
            log.warn("Transfer failed: source and destination wallets are the same. walletId={}",
                    sourceWalletId);
            throw new TransferException("A wallet cannot transfer money to itself");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transfer failed: amount must be greater than zero. sourceWalletId={}, destinationWalletId={}",
                    sourceWalletId, destinationWalletId);
            throw new TransferException("Transfer amount must be greater than zero");
        }

        if (!source.getCurrency().equals(currency)
                || !destination.getCurrency().equals(currency)) {
            log.warn(
                    "Transfer failed: currency mismatch. sourceWalletId={}, destinationWalletId={}, currency={}",
                    sourceWalletId,
                    destinationWalletId,
                    currency
            );
            throw new TransferException("Currency mismatch");
        }

        if (source.getBalance().compareTo(amount) < 0) {
            log.warn(
                    "Transfer failed: insufficient funds. sourceWalletId={}, destinationWalletId={}, amount={}",
                    sourceWalletId,
                    destinationWalletId,
                    amount
            );
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