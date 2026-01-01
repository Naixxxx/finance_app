import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.com.financeApp.core.models.User;
import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.core.repository.WalletRepository;
import org.com.financeApp.infra.InMemoryWalletRepository;
import org.com.financeApp.services.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalletServiceTest {

  private WalletRepository walletRepo;
  private WalletService walletService;

  private User ivan;
  private User anna;

  @BeforeEach
  void setUp() {
    walletRepo = new InMemoryWalletRepository();
    walletService = new WalletService(walletRepo);

    ivan = new User("ivan", "1234");
    anna = new User("anna", "abcd");
  }

  @Test
  void getWallet_shouldCreateAndBindToUserLogin() {
    Wallet w = walletService.getWallet(ivan);

    assertNotNull(w);
    assertEquals("ivan", w.getOwnerLogin());
  }

  @Test
  void differentUsers_shouldHaveDifferentWallets() {
    Wallet w1 = walletService.getWallet(ivan);
    Wallet w2 = walletService.getWallet(anna);

    assertNotNull(w1);
    assertNotNull(w2);
    assertNotSame(w1, w2);
    assertEquals("ivan", w1.getOwnerLogin());
    assertEquals("anna", w2.getOwnerLogin());
  }

  @Test
  void addCategory_shouldAddCategoryToWallet() {
    walletService.addCategory(ivan, "Food");

    Wallet w = walletService.getWallet(ivan);
    assertTrue(w.hasCategory("Food"));
  }

  @Test
  void addCategory_empty_shouldThrow() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> walletService.addCategory(ivan, "   "));
    assertTrue(ex.getMessage().toLowerCase().contains("категор"));
  }

  @Test
  void setBudget_forExistingCategory_shouldSet() {
    walletService.addCategory(ivan, "Food");
    walletService.setBudget(ivan, "Food", 4000);

    Wallet w = walletService.getWallet(ivan);
    assertEquals(4000.0, w.getBudget("Food"));
  }

  @Test
  void setBudget_forMissingCategory_shouldThrow() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> walletService.setBudget(ivan, "Food", 1000));
    assertTrue(ex.getMessage().toLowerCase().contains("не найд"));
  }

  @Test
  void addIncome_shouldAddTransaction_andNoWarningsInitially() {
    walletService.addCategory(ivan, "Salary");

    List<String> warnings =
        walletService.addIncome(ivan, "Salary", 20000, LocalDate.of(2025, 12, 1), "payday");

    Wallet w = walletService.getWallet(ivan);
    assertEquals(1, w.getTransactions().size());
    assertTrue(warnings.isEmpty());
  }

  @Test
  void addExpense_unknownCategory_shouldThrow() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> walletService.addExpense(ivan, "Food", 100, LocalDate.now(), null));
    assertTrue(ex.getMessage().toLowerCase().contains("категор"));
    assertTrue(ex.getMessage().toLowerCase().contains("не найд"));
  }

  @Test
  void addExpense_amountMustBePositive_shouldThrow() {
    walletService.addCategory(ivan, "Food");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> walletService.addExpense(ivan, "Food", 0, LocalDate.now(), null));
    assertTrue(ex.getMessage().toLowerCase().contains("сумм"));
  }

  @Test
  void expenseOverBudget_shouldReturnWarning() {
    walletService.addCategory(ivan, "Food");
    walletService.setBudget(ivan, "Food", 100);

    List<String> warnings =
        walletService.addExpense(ivan, "Food", 150, LocalDate.of(2025, 12, 2), "groceries");

    assertFalse(warnings.isEmpty());
    assertTrue(warnings.stream().anyMatch(s -> s.toLowerCase().contains("превыш")));
  }

  @Test
  void expensesGreaterThanIncome_shouldReturnWarning() {
    walletService.addCategory(ivan, "Food");
    walletService.addCategory(ivan, "Salary");

    walletService.addIncome(ivan, "Salary", 100, LocalDate.of(2025, 12, 1), null);

    List<String> warnings =
        walletService.addExpense(ivan, "Food", 150, LocalDate.of(2025, 12, 2), null);

    assertTrue(
        warnings.stream().anyMatch(s -> s.toLowerCase().contains("расходы превысили доходы")));
  }

  @Test
  void budgetWarning_shouldBeComputedPerCategory() {
    walletService.addCategory(ivan, "Food");
    walletService.addCategory(ivan, "Taxi");
    walletService.setBudget(ivan, "Food", 100);

    List<String> warnings =
        walletService.addExpense(ivan, "Taxi", 150, LocalDate.of(2025, 12, 2), null);

    assertFalse(warnings.stream().anyMatch(s -> s.contains("'Food'")));
  }

  @Test
  void usersShouldNotAffectEachOtherBudgetsOrTransactions() {
    walletService.addCategory(ivan, "Food");
    walletService.setBudget(ivan, "Food", 100);
    walletService.addExpense(ivan, "Food", 150, LocalDate.of(2025, 12, 2), null);

    Wallet wAnna = walletService.getWallet(anna);
    assertTrue(wAnna.getTransactions().isEmpty());
    assertTrue(wAnna.getBudgetsByCategory().isEmpty());
    assertTrue(wAnna.getCategories().isEmpty());
  }
}
