package org.com.financeApp.services;

import org.com.financeApp.core.models.Wallet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

public class ReportService {
    private final StatsService stats;

    public ReportService(StatsService stats) {
        if (stats == null) throw new IllegalArgumentException("StatsService не должен быть null");
        this.stats = stats;
    }

    public String buildReport(Wallet wallet) {
        return buildReport(wallet, null, null);
    }

    public String buildReport(Wallet wallet, LocalDate from, LocalDate to) {
        if (wallet == null) throw new IllegalArgumentException("Wallet не должен быть null");

        var nf = numberFormat();

        double totalIncome = (from == null && to == null)
                ? stats.totalIncome(wallet)
                : stats.totalIncome(wallet, from, to);

        double totalExpense = (from == null && to == null)
                ? stats.totalExpense(wallet)
                : stats.totalExpense(wallet, from, to);

        Map<String, Double> incomeByCat = (from == null && to == null)
                ? stats.incomeByCategory(wallet)
                : stats.incomeByCategory(wallet, from, to);

        Map<String, Double> expenseByCat = (from == null && to == null)
                ? stats.expenseByCategory(wallet)
                : stats.expenseByCategory(wallet, from, to);

        Map<String, StatsService.BudgetStatus> budgetStatus = (from == null && to == null)
                ? stats.budgetStatus(wallet)
                : stats.budgetStatus(wallet, from, to);

        StringBuilder sb = new StringBuilder();

        if (from != null || to != null) {
            sb.append("Период: ")
                    .append(from == null ? "..." : from)
                    .append(" — ")
                    .append(to == null ? "..." : to)
                    .append("\n\n");
        }

        sb.append("Общий доход: ").append(nf.format(totalIncome)).append("\n");
        sb.append("Доходы по категориям:\n");
        if (incomeByCat.isEmpty()) {
            sb.append("- (нет данных)\n");
        } else {
            incomeByCat.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(e -> sb.append("- ").append(e.getKey()).append(": ")
                            .append(nf.format(e.getValue())).append("\n"));
        }

        sb.append("\n");
        sb.append("Общие расходы: ").append(nf.format(totalExpense)).append("\n");
        sb.append("Расходы по категориям:\n");
        if (expenseByCat.isEmpty()) {
            sb.append("- (нет данных)\n");
        } else {
            expenseByCat.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(e -> sb.append("- ").append(e.getKey()).append(": ")
                            .append(nf.format(e.getValue())).append("\n"));
        }

        sb.append("\n");
        sb.append("Бюджет по категориям:\n");
        if (budgetStatus.isEmpty()) {
            sb.append("- (бюджеты не заданы)\n");
        } else {
            budgetStatus.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(e -> {
                        String category = e.getKey();
                        var st = e.getValue();
                        sb.append("- ").append(category)
                                .append(": ").append(nf.format(st.limit()))
                                .append(", Оставшийся бюджет: ").append(nf.format(st.remaining()));
                        if (st.remaining() < 0) sb.append(" (ПРЕВЫШЕН)");
                        sb.append("\n");
                    });
        }

        sb.append("\n");
        sb.append("Баланс: ").append(nf.format(wallet.getBalance())).append("\n");

        sb.append("\n");
        if (totalExpense > totalIncome) {
            sb.append("ВНИМАНИЕ: расходы превысили доходы (")
                    .append(nf.format(totalExpense)).append(" > ").append(nf.format(totalIncome))
                    .append(")\n");
        }

        return sb.toString();
    }

    public void saveToFile(Path path, String report) throws IOException {
        if (path == null) throw new IllegalArgumentException("path не должен быть null");
        if (report == null) throw new IllegalArgumentException("report не должен быть null");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, report);
    }

    private static DecimalFormat numberFormat() {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.0", sym);
        df.setGroupingUsed(true);
        return df;
    }
}
