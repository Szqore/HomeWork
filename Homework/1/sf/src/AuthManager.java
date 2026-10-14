import javax.swing.JOptionPane;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuthManager {

    private static final String DB_URL =
            "jdbc:ucanaccess://C:/Users/D.Nepomnyaschikh/IdeaProjects/sf/Database2.accdb";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    static {
        initUsersTable();
        migratePlainPasswordsToHash();
    }

    private static void initUsersTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            boolean exists = false;
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "Users", null)) {
                if (rs.next()) exists = true;
            }

            if (!exists) {
                stmt.executeUpdate(
                        "CREATE TABLE Users (" +
                                "  id COUNTER PRIMARY KEY, " +
                                "  login TEXT(50) NOT NULL, " +
                                "  password TEXT(120) NOT NULL, " +
                                "  role TEXT(20) NOT NULL, " +
                                "  full_name TEXT(100)" +
                                ")"
                );

                try (PreparedStatement p = conn.prepareStatement(
                        "INSERT INTO Users (login, password, role, full_name) VALUES (?, ?, ?, ?)")) {

                    p.setString(1, "owner");
                    p.setString(2, PasswordUtil.hash("owner123"));
                    p.setString(3, "owner");
                    p.setString(4, "Владелец студии");
                    p.executeUpdate();

                    p.setString(1, "user");
                    p.setString(2, PasswordUtil.hash("user123"));
                    p.setString(3, "user");
                    p.setString(4, "Сотрудник приёмной");
                    p.executeUpdate();

                    p.setString(1, "job");
                    p.setString(2, PasswordUtil.hash("job123"));
                    p.setString(3, "job");
                    p.setString(4, "Мастер-исполнитель");
                    p.executeUpdate();
                }
                System.out.println("[AuthManager] Таблица Users создана + дефолтные аккаунты.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Ошибка при инициализации Users:\n" + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Если в таблице есть открытые пароли — автоматически превращаем в SHA-256 хэши */
    private static void migratePlainPasswordsToHash() {
        String select = "SELECT id, password FROM Users";
        String update = "UPDATE Users SET password = ? WHERE id = ?";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(select);
             PreparedStatement upd = conn.prepareStatement(update)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String pwd = rs.getString("password");
                if (pwd == null) continue;
                if (pwd.length() != 64) {
                    upd.setString(1, PasswordUtil.hash(pwd));
                    upd.setInt(2, id);
                    upd.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Миграция паролей: " + e.getMessage());
        }
    }

    public static User authenticate(String login, String password) {
        String sql = "SELECT id, login, password, role, full_name FROM Users WHERE login = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtil.matches(password, storedHash)) {
                        return new User(
                                rs.getInt("id"),
                                rs.getString("login"),
                                storedHash,
                                rs.getString("role"),
                                rs.getString("full_name")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Ошибка авторизации:\n" + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public static boolean register(String login, String password, String fullName, String role) {
        if (login == null || login.trim().isEmpty()) return false;
        if (password == null || password.length() < 4) return false;
        if (userExists(login)) return false;

        String sql = "INSERT INTO Users (login, password, role, full_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login.trim());
            pstmt.setString(2, PasswordUtil.hash(password));
            pstmt.setString(3, role);
            pstmt.setString(4, fullName);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Не удалось создать аккаунт:\n" + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static boolean userExists(String login) {
        String sql = "SELECT id FROM Users WHERE login = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, login, password, role, full_name FROM Users ORDER BY id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("full_name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка чтения Users: " + e.getMessage());
        }
        return list;
    }

    public static boolean deleteUser(String login) {
        if ("owner".equalsIgnoreCase(login)) return false;

        String sql = "DELETE FROM Users WHERE login = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Ошибка удаления пользователя: " + e.getMessage());
            return false;
        }
    }
}