package org.com.financeApp.infra;

import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.core.repository.WalletRepository;

import java.util.HashMap;
import java.util.Map;

public class InMemoryWalletRepository implements WalletRepository {
    private final Map<String, Wallet> walletsByLogin = new HashMap<>();

    @Override
    public Wallet getOrCreate(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("Логин не должен быть пустым");
        }
        String key = login.trim();
        return walletsByLogin.computeIfAbsent(key, Wallet::new);
    }

    @Override
    public void save(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet не должен быть null");
        }
        walletsByLogin.put(wallet.getOwnerLogin(), wallet);
    }
}
