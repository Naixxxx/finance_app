package org.com.financeApp.services;

import org.com.financeApp.core.models.Transaction;
import org.com.financeApp.core.models.TransactionType;
import org.com.financeApp.core.models.User;
import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.core.repository.WalletRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WalletService {
    private final WalletRepository walletRepo;

    public WalletService(WalletRepository walletRepo) {
        if (walletRepo == null) throw new IllegalArgumentException("WalletRepository не должен быть null");
        this.walletRepo = walletRepo;
    }

    public Wallet getWallet(User user) {
        requireUser(user);
        return walletRepo.getOrCreate(user.getLogin());
    }

    public void addCategory(User user, String category) {
        requireUser(user);
        category = normalize(category);
        if (category.isEmpty()) throw new IllegalArgumentException("Категория не должна быть пустой");

        Wallet wallet = getWallet(user);
        wallet.addCategory(category);
        walletRepo.save(wallet);
    }

    public void setBudget(User user, String category, double limit) {
        requireUser(user);
        category = normalize(category);

        if (category.isEmpty()) throw new IllegalArgumentException("Категория не должна быть пустой");
        if (limit < 0) throw new IllegalArgumentException("Лимит бюджета не может быть отрицательным");

        Wallet wallet = getWallet(user);
        if (!wallet.hasCategory(category)) {
            throw new IllegalArgumentException("Категория не найдена: " + category);
        }

        wallet.setBudget(category, limit);
        walletRepo.save(wallet);
    }

    public List<String> addIncome(User user, String category, double amount, LocalDate date, String comment) {
        return addOperation(user, TransactionType.INCOME, category, amount, date, comment);
    }

    public List<String> addExpense(User user, String category, double amount, LocalDate date, String comment) {
        return addOperation(user, TransactionType.EXPENSE, category, amount, date, comment);
    }

    private List<String> addOperation(User user, TransactionType type, String category, double amount, LocalDate date, String comment) {
        requireUser(user);

        category = normalize(category);
        comment = comment == null ? null : comment.trim();
        if (category.isEmpty()) throw new IllegalArgumentException("Категория не должна быть пустой");
        if (amount <= 0) throw new IllegalArgumentException("Сумма должна быть больше 0");
        if (date == null) date = LocalDate.now();

        Wallet wallet = getWallet(user);
        if (!wallet.hasCategory(category)) {
            if (type == TransactionType.INCOME) {
                wallet.addCategory(category);
                walletRepo.save(wallet);
            } else {
                throw new IllegalArgumentException("Категория не найдена: " + category);
            }
        }

        Transaction tx = new Transaction(type, category, amount, date, comment);
        wallet.addTransaction(tx);
        walletRepo.save(wallet);

        return buildWarnings(wallet, category);
    }

    private List<String> buildWarnings(Wallet wallet, String changedCategory) {
        List<String> warnings = new ArrayList<>();

        Double budget = wallet.getBudget(changedCategory);
        if (budget != null) {
            double spent = wallet.getTransactions().stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .filter(t -> t.getCategory().equals(changedCategory))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double remaining = budget - spent;

            // 80% warning (только если бюджет задан и ещё не превышен)
            if (budget > 0) {
                double threshold = budget * 0.8;
                if (spent >= threshold && spent < budget) {
                    warnings.add("Вы израсходовали " + Math.round((spent / budget) * 100)
                            + "% бюджета по категории '" + changedCategory + "'. Осталось: " + remaining);
                }
            }

            if (remaining < 0) {
                warnings.add("Превышен лимит бюджета по категории '" + changedCategory + "' на " + (-remaining));
            }
        }

        double totalIncome = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpense = wallet.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        if (totalExpense > totalIncome) {
            warnings.add("Расходы превысили доходы (" + totalExpense + " > " + totalIncome + ")");
        }

        return warnings;
    }

    private static void requireUser(User user) {
        if (user == null) throw new IllegalStateException("Пользователь не авторизован");
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}