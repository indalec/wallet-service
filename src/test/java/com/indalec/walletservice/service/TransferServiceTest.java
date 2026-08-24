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

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.of(destination));

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transfer result = transferService.transfer(
                sourceId,
                destinationId,
                new BigDecimal("25.00"),
                "EUR"
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferService.transfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("150.00"),
                        "EUR"
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferService.transfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("25.00"),
                        "EUR"
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferService.transfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("-10.00"),
                        "EUR"
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.of(destination));

        assertThrows(
                TransferException.class,
                () -> transferService.transfer(
                        sourceId,
                        destinationId,
                        BigDecimal.ZERO,
                        "EUR"
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

        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet));

        assertThrows(
                TransferException.class,
                () -> transferService.transfer(
                        walletId,
                        walletId,
                        new BigDecimal("25.00"),
                        "EUR"
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.empty());

        assertThrows(
                WalletNotFoundException.class,
                () -> transferService.transfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("25.00"),
                        "EUR"
                )
        );

        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenDestinationWalletDoesNotExist() {

        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();

        Wallet source = new Wallet(
                "Alice",
                "EUR",
                new BigDecimal("100.00")
        );

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.empty());

        assertThrows(
                WalletNotFoundException.class,
                () -> transferService.transfer(
                        sourceId,
                        destinationId,
                        new BigDecimal("25.00"),
                        "EUR"
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                source.getBalance()
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

        when(walletRepository.findById(sourceId))
                .thenReturn(Optional.of(source));

        when(walletRepository.findById(destinationId))
                .thenReturn(Optional.of(destination));

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transfer result = transferService.transfer(
                sourceId,
                destinationId,
                new BigDecimal("25.00"),
                "EUR"
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