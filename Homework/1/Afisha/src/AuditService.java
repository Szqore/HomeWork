package service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditService {
    private static final String LOG_FILE = "app.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String level, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(formatter);
            String logLine = String.format("[%s] [%s] %s", timestamp, level, message);
            writer.write(logLine);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Ошибка записи лога: " + e.getMessage());
        }
    }
}