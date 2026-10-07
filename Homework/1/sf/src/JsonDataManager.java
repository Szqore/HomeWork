package studio;

import javax.swing.JOptionPane;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер сохранения и загрузки данных через MS Access.
 * Совмещает функции хранения текущих заказов и архивации в JSON-формате прямо в базу данных.
 */
public class JsonDataManager {

    // Жесткий и точный путь к базе данных на основе твоей системной ошибки
    private static final String DB_URL = "jdbc:ucanaccess://C:/Users/D.Nepomnyaschikh/IdeaProjects/sf/Database2.accdb";

    // Вспомогательный метод для открытия соединения с Access
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Сохранение новой заявки в таблицы Orders и создание записи в таблице Archive (роль JSON-архива)
     */
    public static void saveData(List<order> orders, boolean showMsg) {
        if (orders == null || orders.isEmpty()) return;

        // Берем самую последнюю добавленную в список заявку для отправки в базу
        order o = orders.get(orders.size() - 1);

        // SQL для вставки в основную таблицу заказов с автоматическим поиском ID неисправности и срочности
        String insertOrderSql = "INSERT INTO Orders (client_name, client_phone, pc_model, issue_id, urgency_id, description, created_date) " +
                "VALUES (?, ?, ?, " +
                "(SELECT id FROM Issues WHERE name = ?), " +
                "(SELECT id FROM Urgencies WHERE name = ?), ?, ?)";

        String insertArchiveSql = "INSERT INTO Archive (order_id, json_snapshot, archived_at) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Включаем транзакцию, чтобы обе таблицы обновлялись синхронно

            int generatedOrderId = -1;
            Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

            // 1. Сохраняем в таблицу Orders
            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, o.name);
                pstmt.setString(2, o.phone);
                pstmt.setString(3, o.model);
                pstmt.setString(4, o.issue);   // Текстовое название проблемы найдет свой цифровой ID
                pstmt.setString(5, o.urgency); // Текстовый вариант срочности найдет свой цифровой ID
                pstmt.setString(6, o.desc);

                pstmt.setTimestamp(7, currentTimestamp);
                pstmt.executeUpdate();

                // Получаем сгенерированный базой ID заказа
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedOrderId = generatedKeys.getInt(1);
                    }
                }
            }

            // 2. Генерируем Snapshot в формате JSON и пишем в таблицу Archive
            if (generatedOrderId != -1) {
                String jsonSnapshot = oneOrderToJson(generatedOrderId, o);

                try (PreparedStatement pstmtArch = conn.prepareStatement(insertArchiveSql)) {
                    pstmtArch.setInt(1, generatedOrderId);
                    pstmtArch.setString(2, jsonSnapshot);
                    pstmtArch.setTimestamp(3, currentTimestamp);
                    pstmtArch.executeUpdate();
                }
            }

            conn.commit(); // Завершаем транзакцию

            if (showMsg) {
                JOptionPane.showMessageDialog(null, "Успешно сохранено в БД Orders и заархивировано в Archive!", "ОК", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Ошибка при работе с Access:\n" + e.getMessage(), "Ошибка БД", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Загрузка списка всех заявок из базы данных MS Access для вывода на экран в GUI
     */
    public static List<order> loadData() {
        List<order> list = new ArrayList<>();

        // Связываем таблицы через LEFT JOIN, чтобы вытащить текстовые названия вместо ID
        String selectSql = "SELECT o.id, o.client_name, o.client_phone, o.pc_model, i.name as issue_name, u.name as urgency_name, o.description, o.created_date " +
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
                o.name = rs.getString("client_name");
                o.phone = rs.getString("client_phone");
                o.model = rs.getString("pc_model");
                o.issue = rs.getString("issue_name");
                o.urgency = rs.getString("urgency_name");
                o.desc = rs.getString("description");

                Timestamp ts = rs.getTimestamp("created_date");
                if (ts != null) {
                    o.date = ts.toLocalDateTime().format(formatter);
                } else {
                    o.date = "";
                }

                list.add(o);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Не удалось прочитать данные из СУБД:\n" + ex.getMessage(), "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
        return list;
    }

    /**
     * Удаление записи из Access по ID
     */
    public static void deleteOrderFromDb(int id) {
        String deleteSql = "DELETE FROM Orders WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления строки: " + e.getMessage());
        }
    }

    // Вспомогательный метод создания одиночной строки JSON архива для записи в базу
    private static String oneOrderToJson(int id, order o) {
        return "{\"id\":" + id +
                ",\"name\":\"" + o.name.replace("\"", "\\\"") + "\"" +
                ",\"phone\":\"" + o.phone.replace("\"", "\\\"") + "\"" +
                ",\"model\":\"" + o.model.replace("\"", "\\\"") + "\"" +
                ",\"issue\":\"" + o.issue.replace("\"", "\\\"") + "\"" +
                ",\"urgency\":\"" + o.urgency.replace("\"", "\\\"") + "\"" +
                ",\"desc\":\"" + (o.desc != null ? o.desc.replace("\"", "\\\"") : "") + "\"" +
                "}";
    }
}
