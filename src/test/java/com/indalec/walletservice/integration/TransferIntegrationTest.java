package com.indalec.walletservice.integration;

import com.indalec.walletservice.model.Wallet;
import com.indalec.walletservice.repository.WalletRepository;
import com.indalec.walletservice.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

//to run the whole SpringBoot aplication, not an isolated test (can modify database)
@SpringBootTest
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
}