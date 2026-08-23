package com.indalec.walletservice.repository;

import com.indalec.walletservice.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

//Repository will give us the Java methods that subtitute the queries, in that case to work with Wallet Object that the type UUID
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
}
