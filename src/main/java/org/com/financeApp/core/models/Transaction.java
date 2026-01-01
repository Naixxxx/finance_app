package org.com.financeApp.core.models;

import java.time.LocalDate;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final TransactionType type;
    private final String category;
    private final double amount;
    private final LocalDate date;
    private final String comment;

    public Transaction(TransactionType type, String category, double amount, LocalDate date, String comment) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.comment = comment;
    }

    public UUID getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getComment() {
        return comment;
    }
}
