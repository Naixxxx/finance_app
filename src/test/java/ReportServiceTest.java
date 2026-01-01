
import org.com.financeApp.core.models.Transaction;
import org.com.financeApp.core.models.TransactionType;
import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.services.ReportService;
import org.com.financeApp.services.StatsService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    @Test
    void buildReport_shouldContainTotalsAndBudgetRemaining_likeExample() {
        Wallet wallet = new Wallet("ivan");

        wallet.addCategory("Еда");
        wallet.addCategory("Развлечения");
        wallet.addCategory("Коммунальные услуги");
        wallet.addCategory("Такси");
        wallet.addCategory("Зарплата");
        wallet.addCategory("Бонус");

        wallet.addTransaction(tx(TransactionType.EXPENSE, "Еда", 300, LocalDate.of(2025, 12, 1)));
        wallet.addTransaction(tx(TransactionType.EXPENSE, "Еда", 500, LocalDate.of(2025, 12, 2)));
        wallet.addTransaction(tx(TransactionType.EXPENSE, "Развлечения", 3000, LocalDate.of(2025, 12, 3)));
        wallet.addTransaction(tx(TransactionType.EXPENSE, "Коммунальные услуги", 3000, LocalDate.of(2025, 12, 4)));
        wallet.addTransaction(tx(TransactionType.EXPENSE, "Такси", 1500, LocalDate.of(2025, 12, 5)));

        wallet.addTransaction(tx(TransactionType.INCOME, "Зарплата", 20000, LocalDate.of(2025, 12, 1)));
        wallet.addTransaction(tx(TransactionType.INCOME, "Зарплата", 40000, LocalDate.of(2025, 12, 15)));
        wallet.addTransaction(tx(TransactionType.INCOME, "Бонус", 3000, LocalDate.of(2025, 12, 20)));

        wallet.setBudget("Еда", 4000);
        wallet.setBudget("Развлечения", 3000);
        wallet.setBudget("Коммунальные услуги", 2500);

        StatsService stats = new StatsService();
        ReportService reportService = new ReportService(stats);

        String report = reportService.buildReport(wallet);

        assertTrue(report.contains("Общий доход: 63,000.0"));
        assertTrue(report.contains("Общие расходы: 8,300.0"));

        assertTrue(report.contains("Еда: 4,000.0, Оставшийся бюджет: 3,200.0"));
        assertTrue(report.contains("Развлечения: 3,000.0, Оставшийся бюджет: 0.0"));
        assertTrue(report.contains("Коммунальные услуги: 2,500.0, Оставшийся бюджет: -500.0"));
        assertTrue(report.contains("(ПРЕВЫШЕН)"));
    }

    @Test
    void saveToFile_shouldWriteReport() throws Exception {
        StatsService stats = new StatsService();
        ReportService reportService = new ReportService(stats);

        String text = "test report";
        Path tmp = Files.createTempFile("pfm-report-", ".txt");

        reportService.saveToFile(tmp, text);

        String read = Files.readString(tmp);
        assertEquals(text, read);
    }

    private static Transaction tx(TransactionType type, String category, double amount, LocalDate date) {
        return new Transaction(type, category, amount, date, null);
    }
}
