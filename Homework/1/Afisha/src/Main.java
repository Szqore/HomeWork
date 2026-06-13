import repository.FileRepository;
import service.BookingService;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) {
    System.out.println("=== ИНИЦИАЛИЗАЦИЯ СИСТЕМЫ ЯНДЕКС.АФИША ===");
    FileRepository.loadDataFromDisk();

    // Запуск фонового планировщика (шедулера) для контроля времени броней
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(Main::cleanExpired, 1, 1, TimeUnit.SECONDS);

    Scanner scanner = new Scanner(System.in);

    while (true) {
      String currentToken = null;
      boolean isGuest = false;

      // цикл авторизаций
      while (currentToken == null && !isGuest) {
        System.out.println("\n=============================================");
        System.out.println("   ДОБРО ПОЖАЛОВАТЬ НА ЯНДЕКС.АФИШУ");
        System.out.println("=============================================");
        System.out.println("1. Войти под своей учетной записью");
        System.out.println("2. Зарегистрироваться (Покупатель / Организатор)");
        System.out.println("3. Войти как Гость (Без логина и пароля)");
        System.out.println("0. Выйти из программы полностью");
        System.out.print("Выберите действие: ");
        String authChoice = scanner.nextLine();

        if ("0".equals(authChoice)) {
          System.out.println("Завершение работы Афиши. До свидания!");
          scheduler.shutdown();
          System.exit(0);
        }

        if ("3".equals(authChoice)) {
          isGuest = true; // Включаем режим гостя
          System.out.println("[ГОСТЬ] Вход выполнен! Доступен просмотр и прямая покупка.");
          break;
        }

        if ("1".equals(authChoice)) {
          System.out.print("Введите email: "); String email = scanner.nextLine();
          System.out.print("Введите пароль: "); String password = scanner.nextLine();

          // Проверяем логин и пароль в файле users тхт
          for (String userLine : FileRepository.users.values()) {
            String[] u = userLine.split(";");
            if (u.length >= 5) {
              if (u[1].equalsIgnoreCase(email) && u[2].equals(password)) {
                currentToken = u[4]; // Извлекаем токен сессии
                System.out.println("[УСПЕХ] Добро пожаловать, " + email);
                break;
              }
            }
          }
          if (currentToken == null) System.out.println("[ОШИБКА] Неверная почта или пароль!");

        } else if ("2".equals(authChoice)) {
          System.out.print("Введите email для регистрации: ");
          String email = scanner.nextLine();

          // Валидация почты (проверка разрешенных доменов)
          if (!email.endsWith("@gmail.com") && !email.endsWith("@mail.ru")) {
            System.out.println("[ОТКАЗ] Разрешены только почты @gmail.com и @mail.ru!");
            continue;
          }

          System.out.print("Придумайте пароль: ");
          String password = scanner.nextLine();

          //  Секретный код для получения прав Создателя/Организатора
          System.out.print("Если вы Организатор, введите секретный код (или Enter для пропуска): ");
          String secretCode = scanner.nextLine();
          String role = "PARTICIPANT";
          if ("ORG123".equals(secretCode)) {
            role = "ORGANIZER";
            System.out.println("[СИСТЕМА] Секретный код верный! Роль: ОРГАНИЗАТОР.");
          } else {
            System.out.println("[СИСТЕМА] Учетная запись создана. Роль: ПОКУПАТЕЛЬ.");
          }

          String generatedToken = "token_" + UUID.randomUUID().toString().substring(0, 4);
          FileRepository.addUserToDisk(email, password, role, generatedToken);
          currentToken = generatedToken;
        }
      }
      // интрефейс кнопок
      boolean userSessionActive = true;
      while (userSessionActive) {
        String userEmail = isGuest ? "GUEST_USER" : "Неизвестно";
        String userRole = isGuest ? "GUEST" : "Нет роли";

        if (!isGuest) {
          String userLine = FileRepository.users.get(currentToken);
          if (userLine == null) { userSessionActive = false; break; }
          String[] u = userLine.split(";");
          userEmail = u[1];
          userRole = u[3];
        }

        System.out.println("\n=============================================");
        System.out.println("   ГЛАВНОЕ МЕНЮ АФИШИ (Вы: " + userEmail + " [" + userRole + "])");
        System.out.println("=============================================");
        System.out.println("1. Посмотреть список мероприятий и квоты мест");
        if (!isGuest) {
          System.out.println("2. Забронировать билет (Заморозка на 15 минут)");
          System.out.println("3. Оплатить существующую бронь (По ID заказа)");
        }
        System.out.println("4. КУПИТЬ БИЛЕТ НАПРЯМУЮ (Моментальная покупка)");
        System.out.println("5. Контроль на входе (Сканировать/ввести QR-код)");
        System.out.println("9. ПОСМОТРЕТЬ МОИ КУПЛЕННЫЕ БИЛЕТЫ (QR-КОДЫ)");
        System.out.println("10. ПОСМОТРЕТЬ АРХИВНЫЕ КОНЦЕРТЫ (SOLD OUT)");

        // Кнопки 6 и 8 выводятся на экран только для Организатора
        if ("ORGANIZER".equals(userRole)) {
          System.out.println("6. Посмотреть аналитику посещаемости мероприятий");
          System.out.println("8. СОЗДАТЬ НОВОЕ МЕРОПРИЯТИЕ И КВОТУ МЕСТ");
        }

        System.out.println("7. Выйти из учетной записи (Сменить пользователя)");
        System.out.println("0. Завершить работу");
        System.out.print("Выберите пункт меню: ");

        String choice = scanner.nextLine();

        switch (choice) {
          case "1":
            System.out.println("\n--- СПИСОК МЕРОПРИЯТИЙ НА АФИШЕ ---");
            System.out.println("ID | Событие | Зона | Цена | Всего мест | Доступно мест");
            for (String ticketLine : FileRepository.ticketTypes.values()) {
              String[] t = ticketLine.split(";");
              System.out.printf("%s | %s | %s | %s руб. | %s шт. | %s шт.\n", t[0], t[1], t[2], t[3], t[4], t[5]);
            }
            break;

          case "2":
            if (isGuest) { System.out.println("[Ошибка] Гости не могут бронировать."); break; }
            System.out.print("Введите ID мероприятия: "); String bId = scanner.nextLine();
            System.out.print("Сколько мест забронировать: ");
            try {
              BookingService.createBooking(currentToken, bId, Integer.parseInt(scanner.nextLine()));
            } catch (Exception e) { System.out.println("[Ошибка] Неверный ввод количества."); }
            break;

          case "3":
            if (isGuest) { System.out.println("[Ошибка] Оплата брони недоступна гостям."); break; }
            System.out.print("Введите ID вашего заказа: ");
            BookingService.payOrder(scanner.nextLine(), currentToken);
            break;

          case "4":
            System.out.print("Введите ID мероприятия: "); String directId = scanner.nextLine();
            System.out.print("Сколько мест хотите купить: ");
            int count = Integer.parseInt(scanner.nextLine());
            String targetEmail = "";
            if (isGuest) {
              System.out.print("Вы гость. Введите ваш email для чека: ");
              targetEmail = scanner.nextLine();
            } else {
              targetEmail = userEmail;
            }
            BookingService.buyTicketDirectly(currentToken, targetEmail, directId, count);
            break;

          case "5":
            System.out.print("Введите ваш текстовый QR-код: ");
            BookingService.checkIn(scanner.nextLine());
            break;

          case "6":
            if (!"ORGANIZER".equals(userRole)) System.out.println("[ОТКАЗ ДОСТУПА]");
            else showStatistics();
            break;

          case "8":
            if (!"ORGANIZER".equals(userRole)) { System.out.println("[ОТКАЗ ДОСТУПА]"); break; }
            System.out.print("Введите ID билета (ticket_3): "); String newId = scanner.nextLine();
            System.out.print("Название концерта: "); String title = scanner.nextLine();
            System.out.print("Категория (VIP/Обычный): "); String type = scanner.nextLine();
            System.out.print("Цена: "); int price = Integer.parseInt(scanner.nextLine());
            System.out.print("Лимит мест (квота): "); int quota = Integer.parseInt(scanner.nextLine());

            FileRepository.addTicketToDisk(newId, title, type, price, quota);
            break;

          case "9":
            System.out.println("\n--- ВАШИ КУПЛЕННЫЕ БИЛЕТЫ ---");
            int myTicketsCount = 0;
            for (String orderLine : FileRepository.orders) {
              String[] o = orderLine.split(";");
              if (o.length >= 10 && o[1].equalsIgnoreCase(userEmail) && "PAID".equals(o[6])) {
                System.out.printf("Событие: %s (%s) | Кол-во: %s шт. | ВАШ QR-КОД: %s | Прошёл: %s\n", o[3], o[4], o[5], o[8], o[9]);
                myTicketsCount++;
              }
            }
            if (myTicketsCount == 0) System.out.println("У вас пока нет купленных билетов.");
            break;

          case "10":
            System.out.println("\n--- АРХИВ ЗАВЕРШЕННЫХ КОНЦЕРТОВ (SOLD OUT) ---");
            try {
              java.nio.file.Path path = java.nio.file.Paths.get("archiveafisha.txt");
              if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.lines(path).forEach(line -> {
                  String[] t = line.split(";");
                  if (t.length >= 6) System.out.printf("ID: %s | %s (%s) | ВСЕ БИЛЕТЫ РАСПРОДАНЫ!\n", t[0], t[1], t[2]);
                });
              } else { System.out.println("Архив пока пуст."); }
            } catch (Exception e) { System.out.println("Ошибка чтения архива: " + e.getMessage()); }
            break;

          case "7":
            System.out.println("[СИСТЕМА] Вы вышли из аккаунта.");
            userSessionActive = false; // Ломаем сессию, перекидываем в меню авторизации
            break;

          case "0":
            System.out.println("До свидания!");
            scheduler.shutdown();
            System.exit(0);

          default:
            System.out.println("[Ошибка] Такого пункта нет.");
        }
      }
    }
  }
  //Бронирование автоматически аннулируется, если оплата не поступила вовремя
  private static synchronized void cleanExpired() {
    long now = System.currentTimeMillis();
    boolean changed = false;
    for (int i = 0; i < FileRepository.orders.size(); i++) {
      String[] o = FileRepository.orders.get(i).split(";");
      if (o.length >= 10) {
        long orderTime = Long.parseLong(o[7]);
        // Для удобства тестов в консоли бронь сгорает через 20 секунд (20000 мс), в релизе 15 минут
        if ("BOOKED".equals(o[6]) && (now - orderTime > 20000)) {
          o[6] = "EXPIRED";
          FileRepository.orders.set(i, String.join(";", o));
          String ticketTypeId = o[2];
          int count = Integer.parseInt(o[5]);

          String ticketLine = FileRepository.ticketTypes.get(ticketTypeId);
          if (ticketLine != null) {
            String[] t = ticketLine.split(";");
            t[5] = String.valueOf(Integer.parseInt(t[5]) + count); // Возвращаем места в квоту
            FileRepository.ticketTypes.put(ticketTypeId, String.join(";", t));
          }
          changed = true;
          System.out.println("\n[ТАЙМЕР] Внимание! Срок оплаты заказа " + o[0] + " истек. Билеты вернулись в продажу!");
          System.out.print("Выберите пункт меню: ");
        }
      }
    }
    if (changed) { FileRepository.saveOrdersToDisk(); FileRepository.saveTicketsToDisk(); }
  }

  //Вывод процента реального посещения мероприятия для Организатора
  private static void showStatistics() {
    int totalPaidTickets = 0; int realCheckedIn = 0;
    for (String orderLine : FileRepository.orders) {
      String[] o = orderLine.split(";");
      if (o.length >= 10 && "PAID".equals(o[6])) {
        int count = Integer.parseInt(o[5]);
        totalPaidTickets += count; // Считаем проданные
        if ("true".equals(o[9])) realCheckedIn += count; // Считаем дошедших людей
      }
    }
    double percentage = totalPaidTickets == 0 ? 0.0 : ((double) realCheckedIn / totalPaidTickets) * 100;
    System.out.println("=============================================");
    System.out.println("СТАТИСТИКА ПОСЕЩАЕМОСТИ ДЛЯ ОРГАНИЗАТОРА:");
    System.out.println("Всего продано билетов: " + totalPaidTickets + " шт.");
    System.out.println("Реально прошли контроль на входе: " + realCheckedIn + " чел.");
    System.out.printf("Процент реального посещения: %.1f%%\n", percentage);
    System.out.println("=============================================");
  }
}
