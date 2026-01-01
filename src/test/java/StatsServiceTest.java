import org.com.financeApp.core.models.Transaction;
import org.com.financeApp.core.models.TransactionType;
import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.services.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatsServiceTest {

    private StatsService stats;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        stats = new StatsService();
        wallet = new Wallet("ivan");

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
    }

    @Test
    void totals_shouldMatchExample() {
        assertEquals(63000.0, stats.totalIncome(wallet));
        assertEquals(8300.0, stats.totalExpense(wallet));
        assertEquals(54700.0, stats.balance(wallet));
    }

    @Test
    void incomeByCategory_shouldGroupCorrectly() {
        Map<String, Double> byCat = stats.incomeByCategory(wallet);

        assertEquals(60000.0, byCat.get("Зарплата"));
        assertEquals(3000.0, byCat.get("Бонус"));
        assertNull(byCat.get("Еда"));
    }

    @Test
    void expenseByCategory_shouldGroupCorrectly() {
        Map<String, Double> byCat = stats.expenseByCategory(wallet);

        assertEquals(800.0, byCat.get("Еда"));
        assertEquals(3000.0, byCat.get("Развлечения"));
        assertEquals(3000.0, byCat.get("Коммунальные услуги"));
        assertEquals(1500.0, byCat.get("Такси"));
    }

    @Test
    void totalsForSelectedCategories_shouldWork() {
        double selected = stats.totalExpenseForCategories(wallet, List.of("Еда", "Такси"));
        assertEquals(2300.0, selected);
    }

    @Test
    void totalsForSelectedCategories_missingCategory_shouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> stats.totalExpenseForCategories(wallet, List.of("Еда", "Несуществующая"))
        );

        assertTrue(ex.getMessage().toLowerCase().contains("категории не найдены"));
        assertTrue(ex.getMessage().contains("Несуществующая"));
    }

    @Test
    void budgetStatus_shouldMatchExampleRemaining() {
        Map<String, StatsService.BudgetStatus> bs = stats.budgetStatus(wallet);

        assertEquals(4000.0, bs.get("Еда").limit());
        assertEquals(800.0, bs.get("Еда").spent());
        assertEquals(3200.0, bs.get("Еда").remaining());

        assertEquals(3000.0, bs.get("Развлечения").limit());
        assertEquals(3000.0, bs.get("Развлечения").spent());
        assertEquals(0.0, bs.get("Развлечения").remaining());

        assertEquals(2500.0, bs.get("Коммунальные услуги").limit());
        assertEquals(3000.0, bs.get("Коммунальные услуги").spent());
        assertEquals(-500.0, bs.get("Коммунальные услуги").remaining());
    }

    @Test
    void periodFiltering_shouldExcludeTransactionsOutsideRange() {
        wallet.addTransaction(tx(TransactionType.EXPENSE, "Еда", 100, LocalDate.of(2025, 11, 30)));

        double decExpense = stats.totalExpense(wallet, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31));
        assertEquals(8300.0, decExpense);
    }

    @Test
    void invalidDateRange_shouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> stats.totalExpense(wallet, LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 1))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("некорректный период"));
    }

    private static Transaction tx(TransactionType type, String category, double amount, LocalDate date) {
        return new Transaction(type, category, amount, date, null);
    }
}