import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.com.financeApp.core.models.Transaction;
import org.com.financeApp.core.models.TransactionType;
import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.services.WalletFileStorage;
import org.junit.jupiter.api.Test;

class WalletFileStorageTest {

  @Test
  void save_then_loadOrCreate_shouldRestoreWallet() throws Exception {
    Path dir = Files.createTempDirectory("pfm-data-");
    WalletFileStorage storage = new WalletFileStorage(dir);

    Wallet w = new Wallet("ivan");
    w.addCategory("Еда");
    w.addCategory("Зарплата");
    w.setBudget("Еда", 4000);

    w.addTransaction(
        new Transaction(
            TransactionType.INCOME, "Зарплата", 20000, LocalDate.of(2025, 12, 1), "pay"));
    w.addTransaction(
        new Transaction(TransactionType.EXPENSE, "Еда", 500, LocalDate.of(2025, 12, 2), "coffee"));

    storage.save("ivan", w);

    Wallet loaded = storage.loadOrCreate("ivan");

    assertEquals("ivan", loaded.getOwnerLogin());
    assertTrue(loaded.getCategories().contains("Еда"));
    assertEquals(4000.0, loaded.getBudget("Еда"));

    assertEquals(2, loaded.getTransactions().size());
    assertEquals(TransactionType.INCOME, loaded.getTransactions().get(0).getType());
    assertEquals("Зарплата", loaded.getTransactions().get(0).getCategory());
  }

  @Test
  void loadOrCreate_whenNoFile_shouldReturnEmptyWallet() throws Exception {
    Path dir = Files.createTempDirectory("pfm-data-");
    WalletFileStorage storage = new WalletFileStorage(dir);

    Wallet loaded = storage.loadOrCreate("anna");

    assertEquals("anna", loaded.getOwnerLogin());
    assertTrue(loaded.getTransactions().isEmpty());
    assertTrue(loaded.getCategories().isEmpty());
    assertTrue(loaded.getBudgetsByCategory().isEmpty());
  }

  @Test
  void export_then_import_snapshot_shouldRestoreState() throws Exception {
    Path dir = Files.createTempDirectory("pfm-snap-");
    WalletFileStorage storage = new WalletFileStorage(dir);

    Wallet w = new Wallet("ivan");
    w.addCategory("Еда");
    w.setBudget("Еда", 100);
    w.addTransaction(
        new Transaction(TransactionType.EXPENSE, "Еда", 80, LocalDate.of(2025, 12, 1), null));

    Path snap = dir.resolve("snap.json");
    storage.exportSnapshot(snap, w);

    Wallet imported = storage.importSnapshot(snap, "ivan");

    assertTrue(imported.getCategories().contains("Еда"));
    assertEquals(100.0, imported.getBudget("Еда"));
    assertEquals(1, imported.getTransactions().size());
  }
}
