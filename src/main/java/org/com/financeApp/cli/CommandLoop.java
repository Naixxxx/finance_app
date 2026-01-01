package org.com.financeApp.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import org.com.financeApp.core.models.User;
import org.com.financeApp.core.models.Wallet;
import org.com.financeApp.core.repository.WalletRepository;
import org.com.financeApp.services.*;

public class CommandLoop {
  private final AuthorizationService auth;
  private final WalletService walletService;
  private final WalletRepository walletRepo;
  private final WalletFileStorage walletStorage;
  private final ReportService reportService;

  public CommandLoop(
      AuthorizationService auth,
      WalletService walletService,
      WalletRepository walletRepo,
      WalletFileStorage walletStorage,
      ReportService reportService) {
    this.auth = auth;
    this.walletService = walletService;
    this.walletRepo = walletRepo;
    this.walletStorage = walletStorage;
    this.reportService = reportService;
  }

  public void run() {
    System.out.println("Personal Finance Manager (CLI)");
    System.out.println("Введите 'help' для списка команд.");

    Scanner sc = new Scanner(System.in);

    while (true) {
      System.out.print("> ");
      String line = sc.nextLine();
      if (line == null) break;

      line = line.trim();
      if (line.isEmpty()) continue;

      try {
        boolean shouldExit = handle(line);
        if (shouldExit) break;
      } catch (Exception e) {
        System.out.println("Ошибка: " + e.getMessage());
      }
    }
    System.out.println("Пока!");
  }

  private boolean handle(String line) throws IOException {
    String[] tokens = split(line);
    String cmd = tokens[0].toLowerCase();

    switch (cmd) {
      case "help" -> {
        printHelp();
        return false;
      }
      case "register" -> {
        requireArgs(tokens, 3, "register <login> <password>");
        User u = auth.register(tokens[1], tokens[2]);
        Wallet w = walletStorage.loadOrCreate(u.getLogin());
        walletRepo.save(w);
        System.out.println("OK: зарегистрирован и выполнен вход: " + u.getLogin());
        return false;
      }
      case "login" -> {
        requireArgs(tokens, 3, "login <login> <password>");
        User u = auth.login(tokens[1], tokens[2]);

        Wallet loaded = walletStorage.loadOrCreate(u.getLogin());
        walletRepo.save(loaded);

        System.out.println("OK: выполнен вход: " + u.getLogin());
        return false;
      }
      case "logout" -> {
        saveCurrentWalletIfAny();
        auth.logout();
        System.out.println("OK: выход из аккаунта");
        return false;
      }
      case "exit" -> {
        saveCurrentWalletIfAny();
        System.out.println("OK: данные сохранены, выход.");
        return true;
      }

      case "category" -> {
        requireAuth();
        requireArgs(tokens, 2, "category add|list <...>");

        String sub = tokens[1].toLowerCase();
        if ("add".equals(sub)) {
          String name = joinFrom(tokens, 2);
          if (name.isBlank()) throw new IllegalArgumentException("category add <name>");
          walletService.addCategory(auth.getCurrentUser(), name);
          System.out.println("OK: категория добавлена: " + name);
        } else if ("list".equals(sub)) {
          Wallet w = currentWallet();
          if (w.getCategories().isEmpty()) {
            System.out.println("(категорий нет)");
          } else {
            System.out.println("Категории:");
            w.getCategories().stream().sorted().forEach(c -> System.out.println("- " + c));
          }
        } else {
          throw new IllegalArgumentException("Неизвестная команда: category " + sub);
        }
        return false;
      }

      case "budget" -> {
        requireAuth();
        requireArgs(tokens, 2, "budget set|show ...");

        String sub = tokens[1].toLowerCase();
        if ("set".equals(sub)) {
          requireArgs(tokens, 4, "budget set <category> <limit>");
          String category = tokens[2];
          double limit = parsePositiveDouble(tokens[3], "Лимит бюджета должен быть числом >= 0");
          walletService.setBudget(auth.getCurrentUser(), category, limit);
          System.out.println("OK: бюджет установлен: " + category + " = " + limit);
        } else if ("show".equals(sub)) {
          Wallet w = currentWallet();
          Map<String, StatsService.BudgetStatus> st =
              new org.com.financeApp.services.StatsService().budgetStatus(w);

          if (st.isEmpty()) {
            System.out.println("(бюджеты не заданы)");
          } else {
            System.out.println("Бюджет по категориям:");
            st.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                    e -> {
                      var bs = e.getValue();
                      System.out.println(
                          "- "
                              + e.getKey()
                              + ": "
                              + bs.limit()
                              + ", Оставшийся бюджет: "
                              + bs.remaining()
                              + (bs.remaining() < 0 ? " (ПРЕВЫШЕН)" : ""));
                    });
          }
        } else {
          throw new IllegalArgumentException("Неизвестная команда: budget " + sub);
        }
        return false;
      }

      case "income", "expense" -> {
        requireAuth();
        requireArgs(tokens, 2, cmd + " add <category> <amount> [YYYY-MM-DD] [comment...]");

        String sub = tokens[1].toLowerCase();
        if (!"add".equals(sub)) {
          throw new IllegalArgumentException(
              cmd + " add <category> <amount> [YYYY-MM-DD] [comment...]");
        }

        requireArgs(tokens, 4, cmd + " add <category> <amount> [YYYY-MM-DD] [comment...]");
        String category = tokens[2];
        double amount = parsePositiveDouble(tokens[3], "Сумма должна быть числом > 0");

        LocalDate date = null;
        String comment = null;

        if (tokens.length >= 5) {
          LocalDate parsed = tryParseDate(tokens[4]);
          if (parsed != null) {
            date = parsed;
            comment = (tokens.length >= 6) ? joinFrom(tokens, 5) : null;
          } else {
            comment = joinFrom(tokens, 4);
          }
        }

        List<String> warnings;
        if ("income".equals(cmd)) {
          warnings =
              walletService.addIncome(auth.getCurrentUser(), category, amount, date, comment);
        } else {
          warnings =
              walletService.addExpense(auth.getCurrentUser(), category, amount, date, comment);
        }

        System.out.println("OK: операция добавлена");

        if (!warnings.isEmpty()) {
          for (String w : warnings) {
            System.out.println("⚠ " + w);
          }
        }
        return false;
      }

      case "stats" -> {
        requireAuth();
        requireArgs(tokens, 2, "stats show [--from YYYY-MM-DD] [--to YYYY-MM-DD]");

        String sub = tokens[1].toLowerCase();
        if (!"show".equals(sub))
          throw new IllegalArgumentException("stats show [--from YYYY-MM-DD] [--to YYYY-MM-DD]");

        LocalDate from = null;
        LocalDate to = null;

        for (int i = 2; i < tokens.length; i++) {
          if ("--from".equals(tokens[i]) && i + 1 < tokens.length) {
            from = LocalDate.parse(tokens[++i]);
          } else if ("--to".equals(tokens[i]) && i + 1 < tokens.length) {
            to = LocalDate.parse(tokens[++i]);
          } else {
            throw new IllegalArgumentException("stats show [--from YYYY-MM-DD] [--to YYYY-MM-DD]");
          }
        }

        Wallet w = currentWallet();
        String report =
            (from == null && to == null)
                ? reportService.buildReport(w)
                : reportService.buildReport(w, from, to);

        System.out.println(report);
        return false;
      }

      case "snapshot" -> {
        requireAuth();
        requireArgs(tokens, 3, "snapshot export|import <path>");

        String sub = tokens[1].toLowerCase();
        String pathStr = tokens[2];

        if ("export".equals(sub)) {
          Wallet w = currentWallet();
          walletStorage.exportSnapshot(Path.of(pathStr), w);
          System.out.println("OK: snapshot сохранён: " + pathStr);
        } else if ("import".equals(sub)) {
          User u = auth.getCurrentUser();
          Wallet imported = walletStorage.importSnapshot(Path.of(pathStr), u.getLogin());

          walletRepo.save(imported);

          System.out.println("OK: snapshot загружен и применён для пользователя: " + u.getLogin());
        } else {
          throw new IllegalArgumentException("snapshot export|import <path>");
        }
        return false;
      }

      case "report" -> {
        requireAuth();
        requireArgs(tokens, 3, "report file <path> [--from YYYY-MM-DD] [--to YYYY-MM-DD]");

        String sub = tokens[1].toLowerCase();
        if (!"file".equals(sub)) throw new IllegalArgumentException("report file <path> ...");

        String pathStr = tokens[2];
        LocalDate from = null;
        LocalDate to = null;

        for (int i = 3; i < tokens.length; i++) {
          if ("--from".equals(tokens[i]) && i + 1 < tokens.length) {
            from = LocalDate.parse(tokens[++i]);
          } else if ("--to".equals(tokens[i]) && i + 1 < tokens.length) {
            to = LocalDate.parse(tokens[++i]);
          } else {
            throw new IllegalArgumentException(
                "report file <path> [--from YYYY-MM-DD] [--to YYYY-MM-DD]");
          }
        }

        Wallet w = currentWallet();
        String report =
            (from == null && to == null)
                ? reportService.buildReport(w)
                : reportService.buildReport(w, from, to);

        reportService.saveToFile(Path.of(pathStr), report);
        System.out.println("OK: отчёт сохранён в файл: " + pathStr);
        return false;
      }

      default -> throw new IllegalArgumentException("Неизвестная команда. Введите 'help'.");
    }
  }

  private void printHelp() {
    System.out.println(
        """
                Команды:
                  help
                  register <login> <password>
                  login <login> <password>
                  logout
                  exit

                  category add <name>
                  category list

                  budget set <category> <limit>
                  budget show

                  income add <category> <amount> [YYYY-MM-DD] [comment...]
                  expense add <category> <amount> [YYYY-MM-DD] [comment...]

                  stats show [--from YYYY-MM-DD] [--to YYYY-MM-DD]
                  report file <path> [--from YYYY-MM-DD] [--to YYYY-MM-DD]

                  snapshot export <path>
                  snapshot import <path>

                Пример:
                  register ivan 1234
                  category add Еда
                  budget set Еда 4000
                  income add Зарплата 20000 2025-12-01
                  expense add Еда 500 2025-12-02 кофе
                  stats show
                  exit
                """);
  }

  private void requireAuth() {
    if (!auth.isAuthenticated()) {
      throw new IllegalStateException("Сначала выполните login");
    }
  }

  private Wallet currentWallet() {
    User u = auth.getCurrentUser();
    if (u == null) throw new IllegalStateException("Сначала выполните login");
    return walletRepo.getOrCreate(u.getLogin());
  }

  private void saveCurrentWalletIfAny() throws IOException {
    User u = auth.getCurrentUser();
    if (u == null) return;

    Wallet w = walletRepo.getOrCreate(u.getLogin());
    walletStorage.save(u.getLogin(), w);
  }

  private static void requireArgs(String[] tokens, int n, String usage) {
    if (tokens.length < n) throw new IllegalArgumentException("Использование: " + usage);
  }

  private static String[] split(String line) {
    return line.trim().split("\\s+");
  }

  private static String joinFrom(String[] tokens, int startIdx) {
    if (startIdx >= tokens.length) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = startIdx; i < tokens.length; i++) {
      if (i > startIdx) sb.append(' ');
      sb.append(tokens[i]);
    }
    return sb.toString().trim();
  }

  private static double parsePositiveDouble(String s, String errMsg) {
    try {
      double v = Double.parseDouble(s);
      if (v <= 0) throw new IllegalArgumentException(errMsg);
      return v;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(errMsg);
    }
  }

  private static LocalDate tryParseDate(String token) {
    try {
      return LocalDate.parse(token);
    } catch (Exception e) {
      return null;
    }
  }
}
