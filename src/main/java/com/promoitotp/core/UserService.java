package com.promoitotp.core;

import com.promoitotp.dao.UserDao;
import com.promoitotp.model.UserRecord;
import com.promoitotp.util.Tokens;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserRecord register(String login, String password, String role) throws SQLException {
        if (login == null || login.isBlank())
            throw new IllegalArgumentException("Логин не может быть пустым");
        if (password == null || password.length() < 4)
            throw new IllegalArgumentException("Пароль слишком короткий (минимум 4 символа)");

        String normalizedRole = role == null ? "" : role.toUpperCase();
        if (!normalizedRole.equals("ADMIN") && !normalizedRole.equals("USER"))
            throw new IllegalArgumentException("Роль должна быть ADMIN или USER");

        if (normalizedRole.equals("ADMIN") && userDao.hasAdmin())
            throw new IllegalStateException("Администратор уже зарегистрирован");

        if (userDao.findByLogin(login).isPresent())
            throw new IllegalStateException("Пользователь с таким логином уже существует");

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        UserRecord user = new UserRecord(login, hash, normalizedRole);
        userDao.insert(user);
        log.info("Зарегистрирован пользователь login={} role={}", login, normalizedRole);
        return user;
    }

    public String authenticate(String login, String password) throws SQLException {
        if (login == null || login.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("Логин и пароль обязательны");

        UserRecord user = userDao.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Неверный логин или пароль"));

        if (!BCrypt.checkpw(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Неверный логин или пароль");

        log.info("Аутентификация успешна: login={}", login);
        return Tokens.issue(user.getId(), user.getLogin(), user.getRole());
    }

    public List<UserRecord> listUsers() throws SQLException {
        return userDao.listRegularUsers();
    }

    public boolean deleteUser(long id) throws SQLException {
        return userDao.removeById(id);
    }
}
