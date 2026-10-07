package studio;//FRONTEND роль Дверь

import javax.swing.SwingUtilities;

/**
 * Главная точка входа в приложение.
 * Отвечает за запуск графического интерфейса в безопасном потоке Swing.
 */
public class Main {
    public static void main(String[] args) {
        // Запуск интерфейса в потоке обработки событий (EDT) для стабильной работы GUI
        SwingUtilities.invokeLater(() -> new PCRepairStudio().setVisible(true));
    }
}
