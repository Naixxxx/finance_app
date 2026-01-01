package org.com.financeApp.services;

import org.com.financeApp.core.models.Transaction;
import org.com.financeApp.core.models.TransactionType;
import org.com.financeApp.core.models.Wallet;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatsService {
    public record BudgetStatus(double limit, double spent, double remaining) {}

    public double totalIncome(Wallet wallet) {
        return total(wallet, TransactionType.INCOME, null, null, null);
    }

    public double totalExpense(Wallet wallet) {
        return total(wallet, TransactionType.EXPENSE, null, null, null);
    }

    public double balance(Wallet wallet) {
        requireWallet(wallet);
        return wallet.getBalance();
    }

    public double totalIncome(Wallet wallet, LocalDate from, LocalDate to) {
        return total(wallet, TransactionType.INCOME, from, to, null);
    }

    public double totalExpense(Wallet wallet, LocalDate from, LocalDate to) {
        return total(wallet, TransactionType.EXPENSE, from, to, null);
    }

    public double totalIncomeForCategories(Wallet wallet, List<String> categories) {
        return total(wallet, TransactionType.INCOME, null, null, categories);
    }

    public double totalExpenseForCategories(Wallet wallet, List<String> categories) {
        return total(wallet, TransactionType.EXPENSE, null, null, categories);
    }

    public double totalIncome(Wallet wallet, LocalDate from, LocalDate to, List<String> categories) {
        return total(wallet, TransactionType.INCOME, from, to, categories);
    }

    public double totalExpense(Wallet wallet, LocalDate from, LocalDate to, List<String> categories) {
        return total(wallet, TransactionType.EXPENSE, from, to, categories);
    }

    public Map<String, Double> incomeByCategory(Wallet wallet) {
        return byCategory(wallet, TransactionType.INCOME, null, null);
    }

    public Map<String, Double> expenseByCategory(Wallet wallet) {
        return byCategory(wallet, TransactionType.EXPENSE, null, null);
    }

    public Map<String, Double> incomeByCategory(Wallet wallet, LocalDate from, LocalDate to) {
        return byCategory(wallet, TransactionType.INCOME, from, to);
    }

    public Map<String, Double> expenseByCategory(Wallet wallet, LocalDate from, LocalDate to) {
        return byCategory(wallet, TransactionType.EXPENSE, from, to);
    }

    public Map<String, BudgetStatus> budgetStatus(Wallet wallet) {
        return budgetStatus(wallet, null, null);
    }

    public Map<String, BudgetStatus> budgetStatus(Wallet wallet, LocalDate from, LocalDate to) {
        requireWallet(wallet);
        validateDateRange(from, to);

        Map<String, BudgetStatus> result = new HashMap<>();

        for (Map.Entry<String, Double> e : wallet.getBudgetsByCategory().entrySet()) {
            String category = e.getKey();
            double limit = e.getValue() == null ? 0.0 : e.getValue();

            double spent = filteredStream(wallet, TransactionType.EXPENSE, from, to, List.of(category))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            result.put(category, new BudgetStatus(limit, spent, limit - spent));
        }

        return result;
    }

    private double total(Wallet wallet,
                         TransactionType type,
                         LocalDate from,
                         LocalDate to,
                         List<String> categories) {

        requireWallet(wallet);
        validateDateRange(from, to);
        validateCategoriesIfProvided(wallet, categories);

        return filteredStream(wallet, type, from, to, categories)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private Map<String, Double> byCategory(Wallet wallet,
                                           TransactionType type,
                                           LocalDate from,
                                           LocalDate to) {

        requireWallet(wallet);
        validateDateRange(from, to);

        return filteredStream(wallet, type, from, to, null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    private Stream<Transaction> filteredStream(Wallet wallet,
                                               TransactionType type,
                                               LocalDate from,
                                               LocalDate to,
                                               List<String> categories) {

        Stream<Transaction> s = wallet.getTransactions().stream()
                .filter(t -> t.getType() == type);

        if (from != null) {
            s = s.filter(t -> !t.getDate().isBefore(from));
        }
        if (to != null) {
            s = s.filter(t -> !t.getDate().isAfter(to));
        }

        if (categories != null && !categories.isEmpty()) {
            Set<String> catSet = categories.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toSet());

            s = s.filter(t -> catSet.contains(t.getCategory()));
        }

        return s;
    }

    private static void validateCategoriesIfProvided(Wallet wallet, List<String> categories) {
        if (categories == null || categories.isEmpty()) return;

        Set<String> existing = wallet.getCategories();
        List<String> normalized = categories.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .toList();

        List<String> missing = normalized.stream()
                .filter(c -> !existing.contains(c))
                .distinct()
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Категории не найдены: " + String.join(", ", missing));
        }
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Некорректный период: from позже to");
        }
    }

    private static void requireWallet(Wallet wallet) {
        if (wallet == null) throw new IllegalArgumentException("Wallet не должен быть null");
    }
}
