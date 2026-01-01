import static org.junit.jupiter.api.Assertions.*;

import org.com.financeApp.core.models.User;
import org.com.financeApp.core.repository.UserRepository;
import org.com.financeApp.infra.InMemoryUserRepository;
import org.com.financeApp.services.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizationServiceTest {
  private UserRepository repo;
  private AuthorizationService auth;

  @BeforeEach
  void setUp() {
    repo = new InMemoryUserRepository();
    auth = new AuthorizationService(repo);
  }

  @Test
  void register_shouldCreateUser_andAuthenticate() {
    User user = auth.register("ivan", "1234");

    assertNotNull(user);
    assertTrue(repo.exists("ivan"));
    assertTrue(auth.isAuthenticated());
    assertNotNull(auth.getCurrentUser());
    assertEquals("ivan", auth.getCurrentUser().getLogin());
  }

  @Test
  void register_shouldTrimLoginAndPassword() {
    User user = auth.register("  ivan  ", "  1234  ");

    assertNotNull(user);
    assertTrue(repo.exists("ivan"));
    assertEquals("ivan", auth.getCurrentUser().getLogin());
    assertEquals("1234", auth.getCurrentUser().getPassword());
  }

  @Test
  void register_duplicateLogin_shouldThrow() {
    auth.register("ivan", "1234");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> auth.register("ivan", "9999"));

    assertTrue(ex.getMessage().toLowerCase().contains("существ"));
  }

  @Test
  void login_existingUser_withCorrectPassword_shouldAuthenticate() {
    auth.register("ivan", "1234");
    auth.logout();

    User user = auth.login("ivan", "1234");

    assertNotNull(user);
    assertTrue(auth.isAuthenticated());
    assertEquals("ivan", auth.getCurrentUser().getLogin());
  }

  @Test
  void login_userNotFound_shouldThrow() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> auth.login("nope", "1234"));

    assertTrue(ex.getMessage().toLowerCase().contains("не найден"));
  }

  @Test
  void login_wrongPassword_shouldThrow() {
    auth.register("ivan", "1234");
    auth.logout();

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> auth.login("ivan", "0000"));

    assertTrue(ex.getMessage().toLowerCase().contains("неверн"));
  }

  @Test
  void logout_shouldClearAuthentication() {
    auth.register("ivan", "1234");

    auth.logout();

    assertFalse(auth.isAuthenticated());
    assertNull(auth.getCurrentUser());
  }

  @Test
  void register_emptyLogin_shouldThrow() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> auth.register("", "1234"));
    assertTrue(ex.getMessage().toLowerCase().contains("логин"));
  }

  @Test
  void register_loginWithSpaces_shouldThrow() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> auth.register("iv an", "1234"));
    assertTrue(ex.getMessage().toLowerCase().contains("пробел"));
  }
}
