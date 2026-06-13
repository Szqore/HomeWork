package repository;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileRepository {
    public static final Map<String, String> users = new ConcurrentHashMap<>();
    public static final Map<String, String> ticketTypes = new ConcurrentHashMap<>();
    public static final List<String> orders = new CopyOnWriteArrayList<>();
    public static final List<String> ghostOrders = new CopyOnWriteArrayList<>();

    private static final String USERS_FILE = "users.txt";
    private static final String TICKETS_FILE = "concert.txt";
    private static final String ORDERS_FILE = "orders.txt";
    private static final String GHOST_FILE = "ghost.txt";

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
                        "event_1;Концерт Басты;2000;5000;100;50;100;50",
                        "event_2;Выставка Ван Гога;1200;0;100;0;100;0"
                ));
            }
            if (!Files.exists(Paths.get(ORDERS_FILE))) {
                Files.createFile(Paths.get(ORDERS_FILE));
            }
            if (!Files.exists(Paths.get(GHOST_FILE))) {
                Files.createFile(Paths.get(GHOST_FILE));
            }

            users.clear();
            ticketTypes.clear();
            orders.clear();
            ghostOrders.clear();

            for (String line : Files.readAllLines(Paths.get(USERS_FILE))) {
                if (!line.trim().isEmpty()) {
                    String[] p = line.split(";");
                    if (p.length >= 5) users.put(p[4], line);
                }
            }

            for (String line : Files.readAllLines(Paths.get(TICKETS_FILE))) {
                if (!line.trim().isEmpty()) {
                    String[] p = line.split(";");
                    if (p.length >= 8) ticketTypes.put(p[0], line);
                }
            }

            for (String line : Files.readAllLines(Paths.get(ORDERS_FILE))) {
                if (!line.trim().isEmpty()) orders.add(line);
            }

            for (String line : Files.readAllLines(Paths.get(GHOST_FILE))) {
                if (!line.trim().isEmpty()) ghostOrders.add(line);
            }

            System.out.println("[ПЗУ] Все файлы Афиши успешно загружены!");

        } catch (IOException e) {
            System.out.println("Ошибка диска при чтении: " + e.getMessage());
        }
    }

    public static synchronized void addUserToDisk(String email, String password, String role, String token) {
        try {
            String id = String.valueOf(users.size() + 1);
            String userLine = id + ";" + email + ";" + password + ";" + role + ";" + token;
            users.put(token, userLine);

            List<String> allUsers = new ArrayList<>(users.values());
            Files.write(Paths.get(USERS_FILE), allUsers);
            System.out.println("[СИСТЕМА] Пользователь успешно записан на диск!");
        } catch (IOException e) {
            System.out.println("Ошибка записи пользователя: " + e.getMessage());
        }
    }

    public static synchronized void addEventToDisk(String eventId, String eventLine) {
        ticketTypes.put(eventId, eventLine);
        saveTicketsToDisk();
        System.out.println("[СИСТЕМА] Новое мероприятие успешно записано!");
    }

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

    public static synchronized void saveGhostOrdersToDisk() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GHOST_FILE))) {
            for (String ghostLine : ghostOrders) {
                writer.write(ghostLine);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка записи ghost.txt: " + e.getMessage());
        }
    }

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