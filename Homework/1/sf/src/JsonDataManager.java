package studio;

import javax.swing.JOptionPane;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер сохранения и загрузки данных через MS Access.
 * Имеет встроенную защиту от пустых (null) значений в СУБД.
 */
public class JsonDataManager {

    private static final String DB_URL = "jdbc:ucanaccess://C:/Users/D.Nepomnyaschikh/IdeaProjects/sf/Database2.accdb";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Сохранение новой заявки в таблицы Orders и создание записи в таблице Archive
     */
    public static void saveData(List<order> orders, boolean showMsg) {
        if (orders == null || orders.isEmpty()) return;
        
        order o = orders.get(orders.size() - 1);

        String insertOrderSql = "INSERT INTO Orders (client_name, client_phone, pc_model, issue_id, urgency_id, description, created_date) " +
                                "VALUES (?, ?, ?, " +
                                "(SELECT id FROM Issues WHERE name = ?), " +
                                "(SELECT id FROM Urgencies WHERE name = ?), ?, ?)";

        String insertArchiveSql = "INSERT INTO Archive (order_id, json_snapshot, archived_at) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 

            int generatedOrderId = -1;
            Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());

            try (PreparedStatement pstmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, o.name);
                pstmt.setString(2, o.phone);
                pstmt.setString(3, o.model);
                pstmt.setString(4, o.issue);   
                pstmt.setString(5, o.urgency); 
                pstmt.setString(6, o.desc);
                pstmt.setTimestamp(7, currentTimestamp);
                
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedOrderId = generatedKeys.getInt(1);
                    }
                }
            }

            if (generatedOrderId != -1) {
                String jsonSnapshot = oneOrderToJson(generatedOrderId, o);
                
                try (PreparedStatement pstmtArch = conn.prepareStatement(insertArchiveSql)) {
                    pstmtArch.setInt(1, generatedOrderId);
                    pstmtArch.setString(2, jsonSnapshot);
                    pstmtArch.setTimestamp(3, currentTimestamp);
                    pstmtArch.executeUpdate();
                }
            }

            conn.commit(); 

            if (showMsg) {
                JOptionPane.showMessageDialog(null, "Успешно сохранено в БД Orders и заархивировано в Archive!", "ОК", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Ошибка при работе с Access:\n" + e.getMessage(), "Ошибка БД", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Загрузка списка всех заявок из базы данных MS Access с защитой от null
     */
    public static List<order> loadData() {
        List<order> list = new ArrayList<>();
        
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
                
                String clientName = rs.getString("client_name");
                o.name = (clientName != null) ? clientName : "";
                
                String clientPhone = rs.getString("client_phone");
                o.phone = (clientPhone != null) ? clientPhone : "";
                
                String pcModel = rs.getString("pc_model");
                o.model = (pcModel != null) ? pcModel : "";
                
                // ЗАЩИТА ОТ NULL: Если в базе пусто, подставляем дефолтное текстовое значение
                String issueName = rs.getString("issue_name");
                o.issue = (issueName != null) ? issueName : "Не указано";
                
                String urgencyName = rs.getString("urgency_name");
                o.urgency = (urgencyName != null) ? urgencyName : "Обычная";
                
                String description = rs.getString("description");
                o.desc = (description != null) ? description : "";
                
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
