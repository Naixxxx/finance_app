package org.com.financeApp;

import org.com.financeApp.cli.CommandLoop;
import org.com.financeApp.core.repository.UserRepository;
import org.com.financeApp.core.repository.WalletRepository;
import org.com.financeApp.infra.InMemoryUserRepository;
import org.com.financeApp.infra.InMemoryWalletRepository;
import org.com.financeApp.services.*;

public class Main {
  public static void main(String[] args) {
    UserRepository userRepo = new InMemoryUserRepository();
    WalletRepository walletRepo = new InMemoryWalletRepository();

    AuthorizationService authService = new AuthorizationService(userRepo);
    WalletService walletService = new WalletService(walletRepo);

    StatsService statsService = new StatsService();
    ReportService reportService = new ReportService(statsService);

    WalletFileStorage walletStorage = new WalletFileStorage(); // ./data по умолчанию

    CommandLoop loop =
        new CommandLoop(authService, walletService, walletRepo, walletStorage, reportService);

    loop.run();
  }
}
