package com.indalec.walletservice.integration;

import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.repository.WalletRepository;
import com.indalec.walletservice.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

//to run the whole SpringBoot aplication, not an isolated test (can modify database). ActiveProfiles to use de application-test.propierties
@SpringBootTest
@ActiveProfiles("test")
class TransferIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferService transferService;

    @Test
    void shouldTransferMoneyAndPersistNewBalances() {

        Wallet source = walletRepository.save(
                new Wallet("Alice", "EUR", new BigDecimal("100.00"))
        );

        Wallet destination = walletRepository.save(
                new Wallet("Bob", "EUR", new BigDecimal("50.00"))
        );

        transferService.transfer(
                source.getId(),
                destination.getId(),
                new BigDecimal("25.00"),
                "EUR"
        );

        Wallet updatedSource =
                walletRepository.findById(source.getId()).orElseThrow();

        Wallet updatedDestination =
                walletRepository.findById(destination.getId()).orElseThrow();

        assertEquals(
                new BigDecimal("75.00"),
                updatedSource.getBalance()
        );

        assertEquals(
                new BigDecimal("75.00"),
                updatedDestination.getBalance()
        );
    }

    @Test
    void shouldNotAllowTwoConcurrentTransfersToOverspendWallet()
            throws InterruptedException {

        Wallet source = walletRepository.save(
                new Wallet("Alice", "EUR", new BigDecimal("100.00"))
        );

        Wallet destinationA = walletRepository.save(
                new Wallet("Bob", "EUR", new BigDecimal("0.00"))
        );

        Wallet destinationB = walletRepository.save(
                new Wallet("Charlie", "EUR", new BigDecimal("0.00"))
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> transferA = () -> {
            try {
                transferService.transfer(
                        source.getId(),
                        destinationA.getId(),
                        new BigDecimal("80.00"),
                        "EUR"
                );
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        Callable<Boolean> transferB = () -> {
            try {
                transferService.transfer(
                        source.getId(),
                        destinationB.getId(),
                        new BigDecimal("70.00"),
                        "EUR"
                );
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        List<Future<Boolean>> results = executor.invokeAll(
                List.of(transferA, transferB)
        );

        executor.shutdown();

        long successfulTransfers = results.stream()
                .filter(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        assertEquals(1, successfulTransfers);

        Wallet finalSource =
                walletRepository.findById(source.getId()).orElseThrow();

        assertTrue(
                finalSource.getBalance().compareTo(BigDecimal.ZERO) >= 0
        );
    }
}