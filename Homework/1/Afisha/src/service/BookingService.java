package service;

import repository.FileRepository;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class BookingService {

    // Функция бронирования билетов (Временная блокировка квоты)
    public static synchronized void createBooking(String userToken, String ticketTypeId, int count) {
        String userLine = FileRepository.users.get(userToken);
        if (userLine == null) return;
        String[] u = userLine.split(";");
        String userEmail = u[1]; String userRole = u[3];

        // Организатор не может покупать/бронировать билеты на свои события
        if ("ORGANIZER".equals(userRole)) {
            System.out.println("[ОГРАНИЧЕНИЕ] Организатор не может бронировать билеты!");
            return;
        }

        String ticketLine = FileRepository.ticketTypes.get(ticketTypeId);
        if (ticketLine == null) { System.out.println("[ОТКАЗ] Билет не найден!"); return; }
        String[] t = ticketLine.split(";");
        String eventTitle = t[1]; String ticketType = t[2];
        int available = Integer.parseInt(t[5]); // Текущая свободная квота датчика мест

        // Проверяем, хватает ли мест
        if (available < count) { System.out.println("[ОТКАЗ] Нет мест! Осталось: " + available); return; }

        // Откусываем выбранные места из общей доступной кучи
        t[5] = String.valueOf(available - count);
        FileRepository.ticketTypes.put(ticketTypeId, String.join(";", t));

        // Проверяем если места закончились, переносим в архивный файл
        checkAndArchiveTicket(ticketTypeId, t);
        FileRepository.saveTicketsToDisk(); // Сохраняем изменение квоты

        // Генерируем уникальный ID для брони
        String orderId = "order_" + UUID.randomUUID().toString().substring(0, 5);
        long timeNow = System.currentTimeMillis();

        // Собираем подробную строчку заказа: Почта, Название, Статус BOOKED
        String newOrderLine = orderId + ";" + userEmail + ";" + ticketTypeId + ";" + eventTitle + ";" + ticketType + ";" + count + ";BOOKED;" + timeNow + ";NO_QR;false";
        FileRepository.orders.add(newOrderLine);
        FileRepository.saveOrdersToDisk();

        System.out.println("\n[БРОНЬ УСПЕШНА] Места заморожены! ID ЗАКАЗА: " + orderId);
    }

    // Функция моментальной покупки (Прямой проход без стадии брони, доступен Юзерам и Гостям)
    public static synchronized void buyTicketDirectly(String userToken, String guestEmail, String ticketTypeId, int count) {
        String userId = "GUEST";
        String targetEmail = guestEmail;

        // Если токен есть, вытаскиваем данные зарегистрированного юзера
        if (userToken != null) {
            String userLine = FileRepository.users.get(userToken);
            if (userLine != null) {
                String[] u = userLine.split(";");
                if ("ORGANIZER".equals(u[3])) {
                    System.out.println("[ОГРАНИЧЕНИЕ] Организатор не может покупать билеты!");
                    return;
                }
                userId = u[0]; targetEmail = u[1];
            }
        }

        String ticketLine = FileRepository.ticketTypes.get(ticketTypeId);
        if (ticketLine == null) { System.out.println("[ОТКАЗ] Мероприятие не найдено!"); return; }
        String[] t = ticketLine.split(";");
        String eventTitle = t[1]; String ticketType = t[2];
        int available = Integer.parseInt(t[5]);

        if (available < count) { System.out.println("[ОТКАЗ] Мест нет!"); return; }

        // Списываем места из квоты
        t[5] = String.valueOf(available - count);
        FileRepository.ticketTypes.put(ticketTypeId, String.join(";", t));
        checkAndArchiveTicket(ticketTypeId, t);
        FileRepository.saveTicketsToDisk();

        // Сразу генерируем PAID заказ и уникальный QR-код
        String orderId = "order_" + UUID.randomUUID().toString().substring(0, 5);
        String qrCode = "QR_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long timeNow = System.currentTimeMillis();

        String newOrderLine = orderId + ";" + targetEmail + ";" + ticketTypeId + ";" + eventTitle + ";" + ticketType + ";" + count + ";PAID;" + timeNow + ";" + qrCode + ";false";
        FileRepository.orders.add(newOrderLine);
        FileRepository.saveOrdersToDisk();

        System.out.println("\n--------------------------------------------------");
        System.out.println("[ПОКУПКА УСПЕШНА] Чек и билет высланы на почту: " + targetEmail);
        System.out.println("Ваш текстовый QR-код для прохода: " + qrCode);
        System.out.println("--------------------------------------------------");
    }

    // Оплата существующей брони
    public static synchronized void payOrder(String orderId, String userToken) {
        String userLine = FileRepository.users.get(userToken);
        String userEmail = userLine != null ? userLine.split(";")[1] : "гостевую почту";

        boolean found = false;
        for (int i = 0; i < FileRepository.orders.size(); i++) {
            String[] o = FileRepository.orders.get(i).split(";");
            if (o[0].equals(orderId) && "BOOKED".equals(o[6])) {
                o[6] = "PAID"; // Меняем статус
                String qrCode = "QR_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                o[8] = qrCode; // Присваиваем QR

                FileRepository.orders.set(i, String.join(";", o));
                FileRepository.saveOrdersToDisk();

                System.out.println("\n--------------------------------------------------");
                System.out.println("[ОПЛАТА УСПЕШНА] Вам выдан QR-код и чек выслан на: " + userEmail);
                System.out.println("Ваш текстовый QR-код для входа: " + qrCode);
                System.out.println("--------------------------------------------------");
                found = true;
                break;
            }
        }
        if (!found) System.out.println("[ОШИБКА] Бронь не найдена.");
    }

    // КОНТРОЛЬ НА ВХОДЕ
    public static synchronized void checkIn(String qrCode) {
        for (int i = 0; i < FileRepository.orders.size(); i++) {
            String[] o = FileRepository.orders.get(i).split(";");
            if (o.length >= 10) {
                if (o[8].equals(qrCode)) {
                    // ОГРАНИЧЕНИЕ ТЗ: Дважды по одному коду пройти нельзя!
                    if ("true".equals(o[9])) {
                        System.out.println("[КОНТРОЛЬ] ОТКАЗ! Билет уже был погашен! Вход заблокирован.");
                        return;
                    }
                    o[9] = "true"; // Гасим билет в базе данных (меняем false на true)
                    FileRepository.orders.set(i, String.join(";", o));
                    FileRepository.saveOrdersToDisk();
                    System.out.println("[КОНТРОЛЬ] Доступ разрешен! Покупатель: " + o[1]);
                    return;
                }
            }
        }
        System.out.println("[КОНТРОЛЬ] Ошибка: QR-код не найден!");
    }

    // Если мест стало 0, вырезаем концерт из активных и кидаем в archiveafisha тхт
    private static void checkAndArchiveTicket(String ticketId, String[] t) {
        if (Integer.parseInt(t[5]) <= 0) {
            FileRepository.ticketTypes.remove(ticketId); // Удаляем из активной продажи
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("archiveafisha.txt", true))) {
                writer.write(String.join(";", t) + ";[SOLD OUT]");
                writer.newLine();
                System.out.println("\n[АРХИВАЦИЯ] На мероприятие '" + t[1] + "' закончились места! Оно перенесено в archiveafisha.txt.");
            } catch (IOException e) {
                System.out.println("Ошибка записи архивного концерта: " + e.getMessage());
            }
        }
    }
}
