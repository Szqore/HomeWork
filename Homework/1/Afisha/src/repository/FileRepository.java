package repository;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileRepository {
    // Высокопроизводительные кэш-карты в оперативной памяти
    public static final Map<String, String> users = new ConcurrentHashMap<>();
    public static final Map<String, String> ticketTypes = new ConcurrentHashMap<>();
    public static final List<String> orders = new CopyOnWriteArrayList<>();


    private static final String USERS_FILE = "./users.txt";
    private static final String TICKETS_FILE = "./concert.txt";
    private static final String ORDERS_FILE = "./orders.txt";

    // Метод загрузки базы данных с диска (ПЗУ) при старте программы
    public static void loadDataFromDisk() {
        try {
            if (!Files.exists(Paths.get(USERS_FILE))) {
                Files.write(Paths.get(USERS_FILE), Arrays.asList(
                        "1;org@afisha.ru;12345;ORGANIZER;token_org",
                        "2;user@afisha.ru;12345;PARTICIPANT;token_user"
                ));
            }
            if (!Files.exists(Paths.get(TICKETS_FILE))) {
                Files.write(Paths.get(TICKETS_FILE), Arrays.asList(
                        "ticket_1;Концерт Басты;VIP;5000;50;50",
                        "ticket_2;Выставка Ван Гога;Обычный;1200;100;100"
                ));
            }
            if (!Files.exists(Paths.get(ORDERS_FILE))) {
                Files.createFile(Paths.get(ORDERS_FILE));
            }

            // Очищаем кэш памяти перед заполнением
            users.clear();
            ticketTypes.clear();
            orders.clear();

            // Читаем пользователей и укладываем в карту памяти (Ключ Токен)
            for (String line : Files.readAllLines(Paths.get(USERS_FILE))) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(";");
                if (p.length >= 5) users.put(p[4], line);
            }

            // Читаем активные концерты (Ключ  ID билета)
            for (String line : Files.readAllLines(Paths.get(TICKETS_FILE))) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(";");
                if (p.length >= 6) ticketTypes.put(p[0], line);
            }

            // Читаем историю всех заказов
            for (String line : Files.readAllLines(Paths.get(ORDERS_FILE))) {
                if (line.trim().isEmpty()) continue;
                orders.add(line);
            }

            System.out.println("[ПЗУ] Все файлы Афиши успешно загружены!");
        } catch (IOException e) {
            System.out.println("Ошибка диска при чтении: " + e.getMessage());
        }
    }

    // Мгновенная запись зарегистрированного пользователя в файл users тхт
    public static synchronized void addUserToDisk(String email, String password, String role, String token) {
        String id = String.valueOf(users.size() + 1);
        String userLine = id + ";" + email + ";" + password + ";" + role + ";" + token;
        users.put(token, userLine); // Обновляем оперативку

        // Перезаписываем файл актуальным списком
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (String line : users.values()) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("[СИСТЕМА] Пользователь успешно записан на диск в users.txt!");
        } catch (IOException e) {
            System.out.println("Ошибка записи пользователя: " + e.getMessage());
        }
    }

    // Запись нового мероприятия, созданного Организатором, в концертный файл
    public static synchronized void addTicketToDisk(String ticketId, String title, String type, int price, int quota) {
        String ticketLine = ticketId + ";" + title + ";" + type + ";" + price + ";" + quota + ";" + quota;
        ticketTypes.put(ticketId, ticketLine); // Обновляем оперативку

        // Перезаписываем файл concert.txt
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TICKETS_FILE))) {
            for (String line : ticketTypes.values()) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("[СИСТЕМА] Новое мероприятие успешно записано в concert.txt!");
        } catch (IOException e) {
            System.out.println("Ошибка сохранения билета: " + e.getMessage());
        }
    }

    // Метод сохранения списка заказов
    public static synchronized void saveOrdersToDisk() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ORDERS_FILE))) {
            for (String orderLine : orders) {
                writer.write(orderLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка записи базы заказов: " + e.getMessage());
        }
    }

    // Метод сохранения обновленных квот мест у концертов
    public static synchronized void saveTicketsToDisk() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TICKETS_FILE))) {
            for (String ticketLine : ticketTypes.values()) {
                writer.write(ticketLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка записи базы квот: " + e.getMessage());
        }
    }
}
