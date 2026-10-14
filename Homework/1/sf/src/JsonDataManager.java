import javax.swing.JOptionPane;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JsonDataManager {

    private static final String DB_URL =
            "jdbc:ucanaccess://C:/Users/D.Nepomnyaschikh/IdeaProjects/sf/Database2.accdb";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void saveData(List<order> orders, boolean showMsg) {
        if (orders == null || orders.isEmpty()) return;
        order o = orders.get(orders.size() - 1);

        if (orderAlreadyExists(o)) {
            System.out.println("Дубликат, пропускаем.");
            return;
        }

        String insertOrderSql = "INSERT INTO Orders " +
                "(client_name, client_phone, pc_model, issue_id, urgency_id, description, " +
                "created_date, progress, client_login, rating, feedback) " +
                "VALUES (?, ?, ?, " +
                "(SELECT id FROM Issues WHERE name = ?), " +
                "(SELECT id FROM Urgencies WHERE name = ?), ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);
            Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql)) {
                pstmt.setString(1, o.name);
                pstmt.setString(2, o.phone);
                pstmt.setString(3, o.model);
                pstmt.setString(4, o.issue);
                pstmt.setString(5, o.urgency);
                pstmt.setString(6, o.desc);
                pstmt.setTimestamp(7, currentTimestamp);
                pstmt.setInt(8, o.progress);
                pstmt.setString(9, o.clientLogin == null ? "" : o.clientLogin);
                pstmt.setInt(10, o.rating);
                pstmt.setString(11, o.feedback == null ? "" : o.feedback);
                pstmt.executeUpdate();
            }

            if (showMsg) {
                JOptionPane.showMessageDialog(null, "Сохранено в БД!",
                        "ОК", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Ошибка при работе с Access:\n" + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean orderAlreadyExists(order o) {
        String sql = "SELECT COUNT(*) FROM Orders " +
                "WHERE client_name = ? AND client_phone = ? AND pc_model = ? AND created_date >= ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Timestamp recent = Timestamp.valueOf(LocalDateTime.now().minusSeconds(30));
            pstmt.setString(1, o.name);
            pstmt.setString(2, o.phone);
            pstmt.setString(3, o.model);
            pstmt.setTimestamp(4, recent);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { }
        return false;
    }

    public static List<order> loadData() {
        List<order> list = new ArrayList<>();
        String selectSql = "SELECT o.id, o.client_name, o.client_phone, o.pc_model, " +
                "i.name as issue_name, u.name as urgency_name, o.description, o.created_date, " +
                "o.progress, o.client_login, o.rating, o.feedback " +
                "FROM (Orders o " +
                "LEFT JOIN Issues i ON o.issue_id = i.id) " +
                "LEFT JOIN Urgencies u ON o.urgency_id = u.id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            while (rs.next()) {
                order o = new order();
                o.id = rs.getInt("id");
                String s;
                s = rs.getString("client_name");   o.name  = (s != null) ? s : "";
                s = rs.getString("client_phone");  o.phone = (s != null) ? s : "";
                s = rs.getString("pc_model");      o.model = (s != null) ? s : "";
                s = rs.getString("issue_name");    o.issue = (s != null) ? s : "Не указано";
                s = rs.getString("urgency_name");  o.urgency = (s != null) ? s : "Обычная";
                s = rs.getString("description");   o.desc = (s != null) ? s : "";

                Timestamp ts = rs.getTimestamp("created_date");
                o.date = (ts != null) ? ts.toLocalDateTime().format(formatter) : "";

                try { o.progress = rs.getInt("progress"); } catch (SQLException ex) { o.progress = 0; }
                try { s = rs.getString("client_login"); o.clientLogin = (s != null) ? s : ""; } catch (SQLException ex) { o.clientLogin = ""; }
                try { o.rating = rs.getInt("rating"); } catch (SQLException ex) { o.rating = 0; }
                try { s = rs.getString("feedback"); o.feedback = (s != null) ? s : ""; } catch (SQLException ex) { o.feedback = ""; }

                list.add(o);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null,
                    "Не удалось прочитать данные:\n" + ex.getMessage(),
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
        return list;
    }

    public static void deleteOrderFromDb(int id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Orders WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления: " + e.getMessage());
        }
    }

    public static boolean updateProgress(int orderId, int progress) {
        String sql = "UPDATE Orders SET progress = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, progress);
            pstmt.setInt(2, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Не удалось сохранить прогресс:\n" + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static boolean updateFeedback(int orderId, int rating, String feedback) {
        String sql = "UPDATE Orders SET rating = ?, feedback = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rating);
            pstmt.setString(2, feedback == null ? "" : feedback);
            pstmt.setInt(3, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Не удалось сохранить отзыв:\n" + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ================== АРХИВ ==================
    public static boolean moveToArchive(order o) {
        String insertSql = "INSERT INTO ArchiveDone " +
                "(order_id, client_name, client_phone, pc_model, issue, urgency, " +
                "description, created_date, closed_date, master_login, rating, feedback) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(true);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            try (PreparedStatement p = conn.prepareStatement(insertSql)) {
                p.setInt(1, o.id);
                p.setString(2, o.name);
                p.setString(3, o.phone);
                p.setString(4, o.model);
                p.setString(5, o.issue);
                p.setString(6, o.urgency);
                p.setString(7, o.desc);
                p.setTimestamp(8, now);
                p.setTimestamp(9, now);
                p.setString(10, o.clientLogin == null ? "" : o.clientLogin);
                p.setInt(11, o.rating);
                p.setString(12, o.feedback == null ? "" : o.feedback);
                p.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Ошибка переноса в архив: " + e.getMessage());
            return false;
        }
    }

    public static List<order> loadArchive() {
        List<order> list = new ArrayList<>();
        String sql = "SELECT * FROM ArchiveDone ORDER BY closed_date DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            while (rs.next()) {
                order o = new order();
                o.id = rs.getInt("order_id");
                String s;
                s = rs.getString("client_name");   o.name  = (s != null) ? s : "";
                s = rs.getString("client_phone");  o.phone = (s != null) ? s : "";
                s = rs.getString("pc_model");      o.model = (s != null) ? s : "";
                s = rs.getString("issue");         o.issue = (s != null) ? s : "";
                s = rs.getString("urgency");       o.urgency = (s != null) ? s : "";
                s = rs.getString("description");   o.desc = (s != null) ? s : "";

                Timestamp cd = rs.getTimestamp("closed_date");
                o.date = (cd != null) ? cd.toLocalDateTime().format(fmt) : "";
                o.progress = 100;

                s = rs.getString("master_login"); o.clientLogin = (s != null) ? s : "";
                try { o.rating = rs.getInt("rating"); } catch (SQLException e) { o.rating = 0; }
                s = rs.getString("feedback"); o.feedback = (s != null) ? s : "";

                list.add(o);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка чтения архива: " + e.getMessage());
        }
        return list;
    }

    public static List<order> loadArchiveByLogin(String login) {
        List<order> list = new ArrayList<>();
        String sql = "SELECT * FROM ArchiveDone WHERE master_login = ? ORDER BY closed_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);

            try (ResultSet rs = stmt.executeQuery()) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                while (rs.next()) {
                    order o = new order();
                    o.id = rs.getInt("order_id");
                    String s;
                    s = rs.getString("client_name");   o.name  = (s != null) ? s : "";
                    s = rs.getString("client_phone");  o.phone = (s != null) ? s : "";
                    s = rs.getString("pc_model");      o.model = (s != null) ? s : "";
                    s = rs.getString("issue");         o.issue = (s != null) ? s : "";
                    s = rs.getString("urgency");       o.urgency = (s != null) ? s : "";
                    s = rs.getString("description");   o.desc = (s != null) ? s : "";

                    Timestamp cd = rs.getTimestamp("closed_date");
                    o.date = (cd != null) ? cd.toLocalDateTime().format(fmt) : "";
                    o.progress = 100;

                    s = rs.getString("master_login"); o.clientLogin = (s != null) ? s : "";
                    try { o.rating = rs.getInt("rating"); } catch (SQLException e) { o.rating = 0; }
                    s = rs.getString("feedback"); o.feedback = (s != null) ? s : "";

                    list.add(o);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка чтения архива по логину: " + e.getMessage());
        }
        return list;
    }

    public static boolean updateArchiveFeedback(int orderId, int rating, String feedback) {
        String sql = "UPDATE ArchiveDone SET rating = ?, feedback = ? WHERE order_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rating);
            pstmt.setString(2, feedback == null ? "" : feedback);
            pstmt.setInt(3, orderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Ошибка обновления отзыва в архиве: " + e.getMessage());
            return false;
        }
    }
}