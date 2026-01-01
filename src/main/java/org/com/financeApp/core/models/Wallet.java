package org.com.financeApp.core.models;

import java.util.*;

public class Wallet {
  private final String ownerLogin;
  private final List<Transaction> transactions = new ArrayList<>();
  private final Set<String> categories = new HashSet<>();
  private final Map<String, Double> budgetsByCategory = new HashMap<>();

  public Wallet(String ownerLogin) {
    this.ownerLogin = ownerLogin;
  }

  public String getOwnerLogin() {
    return ownerLogin;
  }

  public List<Transaction> getTransactions() {
    return Collections.unmodifiableList(transactions);
  }

  public Set<String> getCategories() {
    return Collections.unmodifiableSet(categories);
  }

  public Map<String, Double> getBudgetsByCategory() {
    return Collections.unmodifiableMap(budgetsByCategory);
  }

  public void addCategory(String name) {
    categories.add(name);
  }

  public boolean hasCategory(String name) {
    return categories.contains(name);
  }

  public void setBudget(String category, double limit) {
    budgetsByCategory.put(category, limit);
  }

  public Double getBudget(String category) {
    return budgetsByCategory.get(category);
  }

  public void addTransaction(Transaction tx) {
    transactions.add(tx);
  }

  public double getBalance() {
    double income =
        transactions.stream()
            .filter(t -> t.getType() == TransactionType.INCOME)
            .mapToDouble(Transaction::getAmount)
            .sum();

    double expense =
        transactions.stream()
            .filter(t -> t.getType() == TransactionType.EXPENSE)
            .mapToDouble(Transaction::getAmount)
            .sum();

    return income - expense;
  }
}
