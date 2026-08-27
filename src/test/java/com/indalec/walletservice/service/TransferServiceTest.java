package com.indalec.walletservice.service;

import com.indalec.walletservice.exception.TransferException;
import com.indalec.walletservice.exception.WalletNotFoundException;
import com.indalec.walletservice.model.Transfer;
import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.repository.TransferRepository;
import com.indalec.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferRepository transferRepository;

    private TransferTransactionService transferTransactionService;

    @BeforeEach
    void setUp() {
        transferTransactionService = new TransferTransactionService(
                walletRepository,
                transferRepository
        );
    }

    @Test
    void shouldTransferMoneySuccessfully() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        Wallet destination = new Wallet(
                "Bob",
                "EUR",
                new BigDecimal("50.00")
        );

        when(walletRepository.findByIdForUpdate(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findByIdForUpdate(destinationId))
                .thenReturn(Optional.of(destination));

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transfer result = transferTransactionService.executeTransfer(
                sourceId,
                destinationId,
                new BigDecimal("25.00"),
                "EUR",
                "test-idempotency-key"
        );

        assertEquals(
                new BigDecimal("75.00"),
                source.getBalance()
        );

        assertEquals(
                new BigDecimal("75.00"),
                destination.getBalance()
        );

        assertNotNull(result);

        assertEquals(
                new BigDecimal("25.00"),
                result.getAmount()
        );

        assertEquals(
                "EUR",
                result.getCurrency()
        );

        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void shouldRejectInsufficientFunds() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        Wallet destination = new Wallet(
                "Bob",
                "EUR",
                new BigDecimal("50.00")
        );

        when(walletRepository.findByIdForUpdate(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findByIdForUpdate(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferTransactionService.executeTransfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("150.00"),
                        "EUR",
                        "test-idempotency-key"
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                source.getBalance()
        );

        assertEquals(
                new BigDecimal("50.00"),
                destination.getBalance()
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldRejectCurrencyMismatch() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        Wallet destination = new Wallet(
                "Bob",
                "USD",
                new BigDecimal("50.00")
        );

        when(walletRepository.findByIdForUpdate(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findByIdForUpdate(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferTransactionService.executeTransfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("25.00"),
                        "EUR",
                        "test-idempotency-key"
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                source.getBalance()
        );

        assertEquals(
                new BigDecimal("50.00"),
                destination.getBalance()
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldRejectNonPositiveAmount() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        Wallet destination = new Wallet(
                "Bob",
                "EUR",
                new BigDecimal("50.00")
        );

        when(walletRepository.findByIdForUpdate(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findByIdForUpdate(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferTransactionService.executeTransfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("-10.00"),
                        "EUR",
                        "test-idempotency-key"
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                source.getBalance()
        );

        assertEquals(
                new BigDecimal("50.00"),
                destination.getBalance()
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldRejectZeroAmount() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        Wallet destination = new Wallet(
                "Bob",
                "EUR",
                new BigDecimal("50.00")
        );

        when(walletRepository.findByIdForUpdate(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findByIdForUpdate(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferTransactionService.executeTransfer(
                        sourceId,
                        destinationId,
                        BigDecimal.ZERO,
                        "EUR",
                        "test-idempotency-key"
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                source.getBalance()
        );

        assertEquals(
                new BigDecimal("50.00"),
                destination.getBalance()
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldRejectTransferToSameWallet() {

        UUID walletId = UUID.randomUUID();

        Wallet wallet = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );


        assertThrows(
                TransferException.class,
                () -> transferTransactionService.executeTransfer(
                        walletId,
                        walletId,
                        new BigDecimal("25.00"),
                        "EUR",
                        "test-idempotency-key"
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                wallet.getBalance()
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenSourceWalletDoesNotExist() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet destination = new Wallet(
                "Bob",
                "EUR",
                new BigDecimal("50.00")
        );

        UUID firstId = sourceId.compareTo(destinationId) < 0
                ? sourceId
                : destinationId;

        UUID secondId = sourceId.compareTo(destinationId) < 0
                ? destinationId
                : sourceId;

        if (firstId.equals(sourceId)) {

            when(walletRepository.findByIdForUpdate(sourceId))
                    .thenReturn(Optional.empty());

        } else {

            when(walletRepository.findByIdForUpdate(destinationId))
                    .thenReturn(Optional.of(destination));

            when(walletRepository.findByIdForUpdate(sourceId))
                    .thenReturn(Optional.empty());
        }

        assertThrows(
                WalletNotFoundException.class,
                () -> transferTransactionService.executeTransfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("25.00"),
                        "EUR",
                        "test-idempotency-key"
                )
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldCreateCompletedTransfer() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        Wallet destination = new Wallet(
                "Bob",
                "EUR",
                new BigDecimal("50.00")
        );

        when(walletRepository.findByIdForUpdate(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findByIdForUpdate(destinationId))
                .thenReturn(Optional.of(destination));

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transfer result = transferTransactionService.executeTransfer(
                sourceId,
                destinationId,
                new BigDecimal("25.00"),
                "EUR",
                "test-idempotency-key"
        );

        assertNotNull(result);

        assertEquals(
                new BigDecimal("25.00"),
                result.getAmount()
        );

        assertEquals(
                "EUR",
                result.getCurrency()
        );

        assertEquals(
                source,
                result.getSourceWallet()
        );

        assertEquals(
                destination,
                result.getDestinationWallet()
        );

        assertEquals(
                com.indalec.walletservice.model.TransferStatus.COMPLETED,
                result.getStatus()
        );

        assertNotNull(result.getCreatedAt());

        verify(transferRepository).save(any(Transfer.class));
    }
}