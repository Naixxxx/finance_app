package org.com.financeApp.services;

import org.com.financeApp.core.models.User;
import org.com.financeApp.core.repository.UserRepository;

public class AuthorizationService {
  private final UserRepository repo;
  private User currentUser;

  public AuthorizationService(UserRepository repo) {
    if (repo == null) {
      throw new IllegalArgumentException("UserRepository не должен быть null");
    }
    this.repo = repo;
  }

  public User register(String login, String password) {
    login = normalize(login);
    password = normalize(password);

    validateLogin(login);
    validatePassword(password);

    if (repo.exists(login)) {
      throw new IllegalArgumentException("Пользователь с таким логином уже существует " + login);
    }

    User user = new User(login, password);
    repo.save(user);
    currentUser = user;
    return user;
  }

  public User login(String login, String password) {
    login = normalize(login);
    password = normalize(password);

    validateLogin(login);
    validatePassword(password);

    User user = repo.find(login);
    if (user == null) {
      throw new IllegalArgumentException("Пользователь не найден " + login);
    }

    if (!user.getPassword().equals(password)) {
      throw new IllegalArgumentException("Неверный пароль");
    }

    currentUser = user;
    return user;
  }

  public void logout() {
    currentUser = null;
  }

  public boolean isAuthenticated() {
    return currentUser != null;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  private static String normalize(String s) {
    return s == null ? "" : s.trim();
  }

  private static void validateLogin(String login) {
    if (login.isEmpty() || login == null) {
      throw new IllegalArgumentException("Логин не должен быть пустым");
    }
    if (login.contains(" ")) {
      throw new IllegalArgumentException("Логин не должен содержать пробелы");
    }
  }

  private static void validatePassword(String password) {
    if (password.isEmpty() || password == null) {
      throw new IllegalArgumentException("Пароль не должен быть пустым");
    }
  }
}
