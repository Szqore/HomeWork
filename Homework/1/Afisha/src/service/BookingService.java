package service;

import repository.FileRepository;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Random;

public class BookingService {

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Random random = new Random();

    private static String generateTicketNumber() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static synchronized void createBooking(String userToken, String eventId, String ticketType, int count) {
        lock.writeLock().lock();
        try {
            String userLine = FileRepository.users.get(userToken);
            if (userLine == null) return;
            String[] u = userLine.split(";");
            String userEmail = u[1];
            String userRole = u[3];

            if ("ORGANIZER".equals(userRole)) {
                System.out.println("[ОГРАНИЧЕНИЕ] Организатор не может бронировать билеты!");
                AuditService.log("BOOKING_FAIL", "Организатор " + userEmail + " пытался забронировать билеты");
                return;
            }

            String eventLine = FileRepository.ticketTypes.get(eventId);
            if (eventLine == null) {
                System.out.println("[ОТКАЗ] Мероприятие не найдено!");
                return;
            }
            String[] t = eventLine.split(";");
            String eventTitle = t[1];

            int available;
            if ("USUAL".equals(ticketType)) {
                available = Integer.parseInt(t[6]);
                if (available <= 0) {
                    System.out.println("[ОТКАЗ] Обычные билеты распроданы!");
                    return;
                }
            } else if ("VIP".equals(ticketType)) {
                available = Integer.parseInt(t[7]);
                if (available <= 0) {
                    System.out.println("[ОТКАЗ] VIP билеты распроданы!");
                    return;
                }
            } else {
                System.out.println("[ОТКАЗ] Неверный тип билета!");
                return;
            }

            if (available < count) {
                System.out.println("[ОТКАЗ] Нет мест! Осталось: " + available);
                AuditService.log("BOOKING_FAIL", "Недостаточно мест для " + userEmail + ", нужно=" + count + ", доступно=" + available);
                return;
            }

            if ("USUAL".equals(ticketType)) {
                t[6] = String.valueOf(available - count);
            } else {
                t[7] = String.valueOf(available - count);
            }
            FileRepository.ticketTypes.put(eventId, String.join(";", t));
            checkAndArchiveEvent(eventId, t);
            FileRepository.saveTicketsToDisk();

            String orderId = "order_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
            long timeNow = System.currentTimeMillis();

            // Формат: 0=orderId, 1=email, 2=eventId, 3=eventTitle, 4=ticketType, 5=count, 6=status, 7=timestamp, 8=ticketNumber, 9=checkedIn
            String newOrderLine = orderId + ";" + userEmail + ";" + eventId + ";" + eventTitle + ";" +
                    ticketType + ";" + count + ";BOOKED;" + timeNow + ";NO_TICKET;false";
            FileRepository.orders.add(newOrderLine);
            FileRepository.saveOrdersToDisk();

            System.out.println("\n[БРОНЬ УСПЕШНА] Места заморожены на 1 минуту! ID ЗАКАЗА: " + orderId);
            AuditService.log("BOOKING_SUCCESS", "Пользователь " + userEmail + " забронировал " + count + " " + ticketType + " билетов на " + eventTitle);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public static synchronized void buyTicketDirectly(String userToken, String userEmail, String eventId, String ticketType, int count, boolean isGuest) {
        lock.writeLock().lock();
        try {
            String targetEmail = userEmail;

            if (userToken != null && !"GUEST".equals(userToken)) {
                String userLine = FileRepository.users.get(userToken);
                if (userLine != null) {
                    String[] u = userLine.split(";");
                    if ("ORGANIZER".equals(u[3])) {
                        System.out.println("[ОГРАНИЧЕНИЕ] Организатор не может покупать билеты!");
                        return;
                    }
                    targetEmail = u[1];
                }
            }

            String eventLine = FileRepository.ticketTypes.get(eventId);
            if (eventLine == null) {
                System.out.println("[ОТКАЗ] Мероприятие не найдено!");
                return;
            }
            String[] t = eventLine.split(";");
            String eventTitle = t[1];

            int available;
            if ("USUAL".equals(ticketType)) {
                available = Integer.parseInt(t[6]);
                if (available <= 0) {
                    System.out.println("[ОТКАЗ] Обычные билеты распроданы!");
                    return;
                }
            } else if ("VIP".equals(ticketType)) {
                available = Integer.parseInt(t[7]);
                if (available <= 0) {
                    System.out.println("[ОТКАЗ] VIP билеты распроданы!");
                    return;
                }
            } else {
                System.out.println("[ОТКАЗ] Неверный тип билета!");
                return;
            }

            if (available < count) {
                System.out.println("[ОТКАЗ] Мест нет! Осталось: " + available);
                AuditService.log("PURCHASE_FAIL", "Недостаточно мест для покупки: " + targetEmail);
                return;
            }

            if ("USUAL".equals(ticketType)) {
                t[6] = String.valueOf(available - count);
            } else {
                t[7] = String.valueOf(available - count);
            }
            FileRepository.ticketTypes.put(eventId, String.join(";", t));
            checkAndArchiveEvent(eventId, t);
            FileRepository.saveTicketsToDisk();

            String orderId = "order_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
            String ticketNumber = generateTicketNumber();
            long timeNow = System.currentTimeMillis();

            String newOrderLine = orderId + ";" + targetEmail + ";" + eventId + ";" + eventTitle + ";" +
                    ticketType + ";" + count + ";PAID;" + timeNow + ";" + ticketNumber + ";false";
            FileRepository.orders.add(newOrderLine);
            FileRepository.saveOrdersToDisk();

            if (isGuest) {
                String ghostOrderLine = targetEmail + ";" + ticketNumber + ";" + eventTitle + ";" + ticketType + ";" + count;
                FileRepository.ghostOrders.add(ghostOrderLine);
                FileRepository.saveGhostOrdersToDisk();
                System.out.println("[ГОСТЬ] Билет записан в ghost.txt");
            }

            System.out.println("\n--------------------------------------------------");
            System.out.println("[ПОКУПКА УСПЕШНА] Чек выслан на почту: " + targetEmail);
            System.out.println("Ваш номер билета для прохода: " + ticketNumber);
            System.out.println("--------------------------------------------------");
            AuditService.log("PURCHASE_SUCCESS", targetEmail + " купил " + count + " " + ticketType + " билет(ов) на " + eventTitle + ", номер билета: " + ticketNumber);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public static synchronized void payOrder(String orderId, String userToken) {
        lock.writeLock().lock();
        try {
            String userLine = FileRepository.users.get(userToken);
            String userEmail = userLine != null ? userLine.split(";")[1] : "неизвестно";

            boolean found = false;
            for (int i = 0; i < FileRepository.orders.size(); i++) {
                String[] o = FileRepository.orders.get(i).split(";");
                if (o.length >= 10 && o[0].equals(orderId) && "BOOKED".equals(o[6])) {
                    o[6] = "PAID";
                    String ticketNumber = generateTicketNumber();
                    o[8] = ticketNumber;

                    FileRepository.orders.set(i, String.join(";", o));
                    FileRepository.saveOrdersToDisk();

                    System.out.println("\n--------------------------------------------------");
                    System.out.println("[ОПЛАТА УСПЕШНА] Вам выдан номер билета: " + ticketNumber);
                    System.out.println("--------------------------------------------------");
                    AuditService.log("PAYMENT_SUCCESS", "Оплачен заказ " + orderId + " пользователем " + userEmail + ", билет №" + ticketNumber);
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("[ОШИБКА] Бронь не найдена.");
                AuditService.log("PAYMENT_FAIL", "Не найден заказ " + orderId + " для оплаты");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static synchronized void checkIn(String ticketNumber) {
        lock.readLock().lock();
        try {
            String cleanNumber = ticketNumber.replace("№", "").trim();

            for (int i = 0; i < FileRepository.orders.size(); i++) {
                String[] o = FileRepository.orders.get(i).split(";");
                if (o.length >= 10 && o[8] != null && o[8].equals(cleanNumber)) {
                    if ("true".equals(o[9])) {
                        System.out.println("[КОНТРОЛЬ] ОТКАЗ! Билет " + cleanNumber + " уже был использован! Вход запрещен.");
                        AuditService.log("CHECKIN_FAIL", "Попытка повторного использования билета " + cleanNumber);
                        return;
                    }
                    o[9] = "true";
                    FileRepository.orders.set(i, String.join(";", o));
                    FileRepository.saveOrdersToDisk();
                    System.out.println("[КОНТРОЛЬ] ДОБРО ПОЖАЛОВАТЬ! Покупатель: " + o[1] + " (билет " + cleanNumber + ")");
                    AuditService.log("CHECKIN_SUCCESS", "Проход по билету " + cleanNumber + " для " + o[1]);
                    return;
                }
            }

            for (int i = 0; i < FileRepository.ghostOrders.size(); i++) {
                String[] g = FileRepository.ghostOrders.get(i).split(";");
                if (g.length >= 2 && g[1].equals(cleanNumber)) {
                    System.out.println("[КОНТРОЛЬ] ДОБРО ПОЖАЛОВАТЬ! Гость: " + g[0] + " (билет " + cleanNumber + ")");
                    AuditService.log("CHECKIN_SUCCESS", "Проход гостя по билету " + cleanNumber + " для " + g[0]);
                    return;
                }
            }

            System.out.println("[КОНТРОЛЬ] Ошибка: билет \"" + cleanNumber + "\" не найден!");
            AuditService.log("CHECKIN_FAIL", "Не найден билет с номером " + cleanNumber);

        } finally {
            lock.readLock().unlock();
        }
    }

    private static void checkAndArchiveEvent(String eventId, String[] t) {
        int usualAvailable = Integer.parseInt(t[6]);
        int vipAvailable = Integer.parseInt(t[7]);

        if (usualAvailable <= 0 && vipAvailable <= 0) {
            FileRepository.ticketTypes.remove(eventId);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("archiveafisha.txt", true))) {
                writer.write(String.join(";", t) + ";[SOLD OUT]");
                writer.newLine();
                System.out.println("\n[АРХИВАЦИЯ] На мероприятие '" + t[1] + "' закончились все места! Перенесено в архив.");
                AuditService.log("ARCHIVE", "Событие " + t[1] + " полностью распродано и перемещено в архив");
            } catch (IOException e) {
                System.out.println("Ошибка записи архивного концерта: " + e.getMessage());
            }
        }
    }
}