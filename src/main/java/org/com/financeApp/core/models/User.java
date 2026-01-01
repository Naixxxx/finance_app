package org.com.financeApp.core.models;

public class User {
  private final String login;
  private final String password;

  public User(final String login, final String password) {
    if (login == null || login.trim().isEmpty()) {
      throw new IllegalArgumentException("Логин не должен быть пустым");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("Пароль не должен быть пустым");
    }
    this.password = password;
    this.login = login;
  }

  public String getPassword() {
    return password;
  }

  public String getLogin() {
    return login;
  }
}
