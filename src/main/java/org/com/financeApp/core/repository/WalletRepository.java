package org.com.financeApp.core.repository;

import org.com.financeApp.core.models.Wallet;

public interface WalletRepository {
  Wallet getOrCreate(String login);

  void save(Wallet wallet);
}
