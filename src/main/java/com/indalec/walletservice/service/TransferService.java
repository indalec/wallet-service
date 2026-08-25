package com.indalec.walletservice.service;

import com.indalec.walletservice.exception.TransferException;
import com.indalec.walletservice.model.Transfer;
import com.indalec.walletservice.repository.TransferRepository;
import com.indalec.walletservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger logger =
            LoggerFactory.getLogger(TransferService.class);

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final TransferTransactionService transferTransactionService;

    public TransferService(
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            TransferTransactionService transferTransactionService
    ) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.transferTransactionService = transferTransactionService;
    }

    public Transfer transfer(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {

        logger.info(
                "Transfer started: source={}, destination={}, amount={}, currency={}",
                sourceWalletId,
                destinationWalletId,
                amount,
                currency
        );

        // First check: if this request was already processed, return the existing transfer.
        Transfer existingTransfer =
                transferRepository.findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existingTransfer != null) {

            logger.info(
                    "Duplicate transfer request detected: idempotencyKey={}, transferId={}",
                    idempotencyKey,
                    existingTransfer.getId()
            );

            return existingTransfer;
        }

        try {

            Transfer transfer = transferTransactionService.executeTransfer(
                    sourceWalletId,
                    destinationWalletId,
                    amount,
                    currency,
                    idempotencyKey
            );

            logger.info(
                    "Transfer completed: transferId={}",
                    transfer.getId()
            );

            return transfer;

        } catch (DataIntegrityViolationException e) {

            // Another concurrent request may have created the transfer
            // with the same idempotency key.
            logger.info(
                    "Concurrent duplicate transfer detected: idempotencyKey={}",
                    idempotencyKey
            );

            return findExistingTransfer(idempotencyKey);
        }
    }

    public Transfer findExistingTransfer(String idempotencyKey) {
        return transferRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() ->
                        new TransferException("Transfer not found after concurrent request"));
    }
}