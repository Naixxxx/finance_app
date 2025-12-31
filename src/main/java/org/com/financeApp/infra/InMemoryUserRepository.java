package org.com.financeApp.infra;

import org.com.financeApp.core.models.User;
import org.com.financeApp.core.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> usersByLogin = new HashMap<>();

    @Override
    public boolean exists(String login) {
        if (login == null ) {
            return false;
        }
        return  usersByLogin.containsKey(login.trim());
    }

    @Override
    public User find(String login) {
        if (login == null ) {
            return null;
        }
        return  usersByLogin.get(login.trim());
    }
    @Override
    public void save(User user) {
        if (user == null ) {
            throw new IllegalArgumentException("User должен быть не null");
        }
        String login = user.getLogin();
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("User.login не должен быть пустым");
        }

        usersByLogin.put(login.trim(), user);
    }
}
