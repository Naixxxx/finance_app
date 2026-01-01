package org.com.financeApp.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.com.financeApp.core.models.Transaction;
import org.com.financeApp.core.models.TransactionType;
import org.com.financeApp.core.models.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class WalletFileStorage {
    private final Path baseDir;
    private final ObjectMapper mapper;

    public WalletFileStorage(Path baseDir) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir не должен быть null");
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public WalletFileStorage() {
        this(Path.of("data"));
    }

    public void save(String login, Wallet wallet) throws IOException {
        login = normalizeLogin(login);
        if (wallet == null) throw new IllegalArgumentException("wallet не должен быть null");

        Files.createDirectories(baseDir);

        Path file = filePath(login);
        WalletSnapshot snapshot = WalletSnapshot.fromWallet(wallet);

        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), snapshot);
    }

    public void exportSnapshot(Path file, Wallet wallet) throws IOException {
        if (file == null) throw new IllegalArgumentException("file не должен быть null");
        if (wallet == null) throw new IllegalArgumentException("wallet не должен быть null");

        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        WalletSnapshot snapshot = WalletSnapshot.fromWallet(wallet);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), snapshot);
    }

    public Wallet importSnapshot(Path file, String login) throws IOException {
        if (file == null) throw new IllegalArgumentException("file не должен быть null");
        if (login == null || login.trim().isEmpty()) throw new IllegalArgumentException("login не должен быть пустым");

        WalletSnapshot snapshot = mapper.readValue(file.toFile(), WalletSnapshot.class);
        return snapshot.toWallet(login.trim());
    }

    public Wallet loadOrCreate(String login) throws IOException {
        login = normalizeLogin(login);

        Path file = filePath(login);
        if (!Files.exists(file)) {
            return new Wallet(login);
        }

        WalletSnapshot snapshot = mapper.readValue(file.toFile(), WalletSnapshot.class);
        return snapshot.toWallet(login);
    }

    private Path filePath(String login) {
        return baseDir.resolve(login + ".json");
    }

    private static String normalizeLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("login не должен быть пустым");
        }
        return login.trim();
    }

    public static class WalletSnapshot {
        public final String ownerLogin;
        public final List<String> categories;
        public final Map<String, Double> budgetsByCategory;
        public final List<TransactionSnapshot> transactions;

        @JsonCreator
        public WalletSnapshot(
                @JsonProperty("ownerLogin") String ownerLogin,
                @JsonProperty("categories") List<String> categories,
                @JsonProperty("budgetsByCategory") Map<String, Double> budgetsByCategory,
                @JsonProperty("transactions") List<TransactionSnapshot> transactions
        ) {
            this.ownerLogin = ownerLogin;
            this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
            this.budgetsByCategory = budgetsByCategory == null ? new HashMap<>() : new HashMap<>(budgetsByCategory);
            this.transactions = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
        }

        public static WalletSnapshot fromWallet(Wallet wallet) {
            return new WalletSnapshot(
                    wallet.getOwnerLogin(),
                    new ArrayList<>(wallet.getCategories()),
                    new HashMap<>(wallet.getBudgetsByCategory()),
                    wallet.getTransactions().stream().map(TransactionSnapshot::fromTx).toList()
            );
        }

        public Wallet toWallet(String login) {
            Wallet wallet = new Wallet(login);

            for (String c : categories) {
                if (c != null && !c.trim().isEmpty()) wallet.addCategory(c.trim());
            }

            for (var e : budgetsByCategory.entrySet()) {
                String cat = e.getKey();
                Double lim = e.getValue();
                if (cat != null && !cat.trim().isEmpty() && lim != null) {
                    wallet.setBudget(cat.trim(), lim);
                }
            }

            for (TransactionSnapshot ts : transactions) {
                wallet.addTransaction(ts.toTx());
            }

            return wallet;
        }
    }

    public static class TransactionSnapshot {
        public final TransactionType type;
        public final String category;
        public final double amount;
        public final LocalDate date;
        public final String comment;

        @JsonCreator
        public TransactionSnapshot(
                @JsonProperty("type") TransactionType type,
                @JsonProperty("category") String category,
                @JsonProperty("amount") double amount,
                @JsonProperty("date") LocalDate date,
                @JsonProperty("comment") String comment
        ) {
            this.type = type;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.comment = comment;
        }

        public static TransactionSnapshot fromTx(Transaction tx) {
            return new TransactionSnapshot(
                    tx.getType(),
                    tx.getCategory(),
                    tx.getAmount(),
                    tx.getDate(),
                    tx.getComment()
            );
        }

        public Transaction toTx() {
            return new Transaction(type, category, amount, date, comment);
        }
    }
}
