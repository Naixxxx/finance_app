package org.com.financeApp.core.repository;

import org.com.financeApp.core.models.User;

public interface UserRepository {
    boolean exists(String login);
    User find(String login);
    void save (User user);
}
