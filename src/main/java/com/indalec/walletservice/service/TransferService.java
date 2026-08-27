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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        String requestHash = generateRequestHash(
                sourceWalletId,
                destinationWalletId,
                amount,
                currency
        );

        // First check: if this request was already processed, validate
        // that the idempotency key is being reused for the same operation.
        Transfer existingTransfer =
                transferRepository.findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existingTransfer != null) {

            if (!existingTransfer.getRequestHash().equals(requestHash)) {
                logger.warn(
                        "Idempotency key reused for a different transfer: idempotencyKey={}",
                        idempotencyKey
                );

                throw new TransferException(
                        "Idempotency key already used for a different transfer"
                );
            }

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
                    idempotencyKey,
                    requestHash
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

            Transfer concurrentTransfer = findExistingTransfer(idempotencyKey);

            if (!concurrentTransfer.getRequestHash().equals(requestHash)) {
                throw new TransferException(
                        "Idempotency key already used for a different transfer"
                );
            }

            return concurrentTransfer;
        }
    }

    public Transfer findExistingTransfer(String idempotencyKey) {
        return transferRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() ->
                        new TransferException(
                                "Transfer not found after concurrent request"
                        ));
    }

    private String generateRequestHash(
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency
    ) {
        try {
            String input =
                    sourceWalletId + "|" +
                            destinationWalletId + "|" +
                            amount.toPlainString() + "|" +
                            currency;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }
}