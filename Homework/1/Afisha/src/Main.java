import repository.FileRepository;
import service.BookingService;
import service.AuditService;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class Main {
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static String currentGuestEmail = null;

  public static void main(String[] args) {
    System.out.println("=== ИНИЦИАЛИЗАЦИЯ СИСТЕМЫ НЕДОЯНДЕКС.АФИША ===");
    AuditService.log("INFO", "Приложение запущено");
    FileRepository.loadDataFromDisk();

    scheduler.scheduleAtFixedRate(Main::cleanExpired, 1, 1, TimeUnit.MINUTES);

    Scanner scanner = new Scanner(System.in);

    while (true) {
      String currentToken = null;
      boolean isGuest = false;
      currentGuestEmail = null;

      while (currentToken == null && !isGuest) {
        System.out.println("\n=============================================");
        System.out.println("   ДОБРО ПОЖАЛОВАТЬ НА НЕДОЯНДЕКС.АФИШУ");
        System.out.println("=============================================");
        System.out.println("1. Войти под своей учетной записью");
        System.out.println("2. Зарегистрироваться (Покупатель / Организатор)");
        System.out.println("3. Войти как Гость (Без логина и пароля)");
        System.out.println("0. Выйти из программы полностью");
        System.out.print("Выберите действие: ");

        String authChoice = safeReadLine(scanner);

        if ("0".equals(authChoice)) {
          System.out.println("Завершение работы Афиши. До свидания!");
          AuditService.log("INFO", "Приложение завершено");
          scheduler.shutdown();
          System.exit(0);
        }

        if ("3".equals(authChoice)) {
          isGuest = true;
          System.out.print("Введите ваш email (для получения билетов в ghost.txt): ");
          currentGuestEmail = safeReadLine(scanner);

          if (!currentGuestEmail.endsWith("@gmail.com") && !currentGuestEmail.endsWith("@mail.ru")) {
            System.out.println("[ОТКАЗ] Гостям разрешены только почты @gmail.com и @mail.ru!");
            isGuest = false;
            continue;
          }

          System.out.println("[ГОСТЬ] Вход выполнен! Ваш email: " + currentGuestEmail);
          AuditService.log("GUEST_LOGIN", "Гость вошел с email: " + currentGuestEmail);
          break;
        }

        if ("1".equals(authChoice)) {
          System.out.print("Введите email: ");
          String email = safeReadLine(scanner);
          System.out.print("Введите пароль: ");
          String password = safeReadLine(scanner);

          boolean found = false;
          for (String userLine : FileRepository.users.values()) {
            String[] u = userLine.split(";");
            if (u.length >= 5) {
              if (u[1].equalsIgnoreCase(email) && u[2].equals(password)) {
                currentToken = u[4];
                System.out.println("[УСПЕХ] Добро пожаловать, " + email);
                AuditService.log("LOGIN", "Пользователь " + email + " вошел в систему");
                found = true;
                break;
              }
            }
          }
          if (!found) {
            System.out.println("[ОШИБКА] Неверная почта или пароль!");
            System.out.println("[СОВЕТ] Если у вас нет аккаунта, зарегистрируйтесь (пункт 2)");
            AuditService.log("LOGIN_FAIL", "Неудачная попытка входа для email: " + email);
          }

        } else if ("2".equals(authChoice)) {
          System.out.print("Введите email для регистрации: ");
          String email = safeReadLine(scanner);

          if (!email.endsWith("@gmail.com") && !email.endsWith("@mail.ru")) {
            System.out.println("[ОТКАЗ] Разрешены только почты @gmail.com и @mail.ru!");
            AuditService.log("REG_FAIL", "Попытка регистрации с недопустимым email: " + email);
            continue;
          }

          // ПРОВЕРКА: ЕСТЬ ЛИ УЖЕ ТАКОЙ EMAIL
          boolean emailExists = false;
          for (String userLine : FileRepository.users.values()) {
            String[] u = userLine.split(";");
            if (u.length >= 5 && u[1].equalsIgnoreCase(email)) {
              emailExists = true;
              break;
            }
          }

          if (emailExists) {
            System.out.println("[ОШИБКА] Пользователь с email '" + email + "' уже существует!");
            System.out.println("[СОВЕТ] Используйте '1. Войти' для входа в аккаунт.");
            AuditService.log("REG_FAIL", "Попытка регистрации с существующим email: " + email);
            continue;
          }

          System.out.print("Придумайте пароль: ");
          String password = safeReadLine(scanner);

          System.out.print("Если вы Организатор, введите секретный код (или Enter для пропуска): ");
          String secretCode = safeReadLine(scanner);
          String role = "PARTICIPANT";
          if ("ORG123".equals(secretCode)) {
            role = "ORGANIZER";
            System.out.println("[СИСТЕМА] Секретный код верный! Роль: ОРГАНИЗАТОР.");
          } else {
            System.out.println("[СИСТЕМА] Учетная запись создана. Роль: ПОКУПАТЕЛЬ.");
          }

          String generatedToken = "token_" + UUID.randomUUID().toString().substring(0, 8);
          FileRepository.addUserToDisk(email, password, role, generatedToken);
          currentToken = generatedToken;
          AuditService.log("REG_SUCCESS", "Зарегистрирован новый пользователь: " + email + " (роль: " + role + ")");
        }
      }

      boolean userSessionActive = true;
      while (userSessionActive) {
        String userEmail = isGuest ? currentGuestEmail : "Неизвестно";
        String userRole = isGuest ? "GUEST" : "Нет роли";

        if (!isGuest && currentToken != null) {
          String userLine = FileRepository.users.get(currentToken);
          if (userLine == null) {
            userSessionActive = false;
            break;
          }
          String[] u = userLine.split(";");
          userEmail = u[1];
          userRole = u[3];
        }

        System.out.println("\n=============================================");
        System.out.println("   ГЛАВНОЕ МЕНЮ АФИШИ (Вы: " + userEmail + " [" + userRole + "])");
        System.out.println("=============================================");
        System.out.println("1. Посмотреть список мероприятий и квоты мест");
        if (!isGuest) {
          System.out.println("2. Забронировать билет (Заморозка на 1 минуту)");
          System.out.println("3. Оплатить существующую бронь (По ID заказа)");
        }
        System.out.println("4. КУПИТЬ БИЛЕТ НАПРЯМУЮ (Моментальная покупка)");
        System.out.println("5. Контроль на входе (Ввести номер билета)");
        System.out.println("9. ПОСМОТРЕТЬ МОИ КУПЛЕННЫЕ БИЛЕТЫ");
        System.out.println("10. ПОСМОТРЕТЬ АРХИВНЫЕ КОНЦЕРТЫ (SOLD OUT)");

        if ("ORGANIZER".equals(userRole)) {
          System.out.println("6. Посмотреть аналитику посещаемости мероприятий");
          System.out.println("8. СОЗДАТЬ НОВОЕ МЕРОПРИЯТИЕ");
          System.out.println("11. РЕДАКТИРОВАТЬ МЕРОПРИЯТИЕ (цены/места/удалить тип)");
        }

        System.out.println("7. Выйти из учетной записи (Сменить пользователя)");
        System.out.println("0. Завершить работу");
        System.out.print("Выберите пункт меню: ");

        String choice = safeReadLine(scanner);

        try {
          switch (choice) {
            case "1":
              showEvents();
              break;

            case "2":
              if (isGuest) {
                System.out.println("[Ошибка] Гости не могут бронировать.");
                break;
              }
              selectEventForBooking(scanner, currentToken);
              break;

            case "3":
              if (isGuest) {
                System.out.println("[Ошибка] Оплата брони недоступна гостям.");
                break;
              }
              System.out.print("Введите ID вашего заказа: ");
              BookingService.payOrder(safeReadLine(scanner), currentToken);
              break;

            case "4":
              String targetEmailForPurchase = isGuest ? currentGuestEmail :
                      (currentToken != null && FileRepository.users.get(currentToken) != null ?
                       FileRepository.users.get(currentToken).split(";")[1] : userEmail);
              selectEventForPurchase(scanner, currentToken, isGuest, targetEmailForPurchase);
              break;

            case "5":
              System.out.print("Введите номер билета: ");
              BookingService.checkIn(safeReadLine(scanner));
              break;

            case "6":
              if (!"ORGANIZER".equals(userRole)) {
                System.out.println("[ОТКАЗ ДОСТУПА]");
              } else {
                showStatistics();
              }
              break;

            case "8":
              if (!"ORGANIZER".equals(userRole)) {
                System.out.println("[ОТКАЗ ДОСТУПА]");
                break;
              }
              createNewEvent(scanner, userEmail);
              break;

            case "11":
              if (!"ORGANIZER".equals(userRole)) {
                System.out.println("[ОТКАЗ ДОСТУПА]");
                break;
              }
              editEvent(scanner);
              break;

            case "9":
              showMyTickets(userEmail, isGuest);
              break;

            case "10":
              showArchive();
              break;

            case "7":
              System.out.println("[СИСТЕМА] Вы вышли из аккаунта.");
              AuditService.log("LOGOUT", "Пользователь " + userEmail + " вышел из системы");
              userSessionActive = false;
              break;

            case "0":
              System.out.println("До свидания!");
              AuditService.log("INFO", "Приложение завершено");
              scheduler.shutdown();
              System.exit(0);

            default:
              System.out.println("[Ошибка] Такого пункта нет.");
          }
        } catch (Exception e) {
          System.out.println("[ОШИБКА] " + e.getMessage());
          AuditService.log("ERROR", "Ошибка при выполнении операции: " + e.getMessage());
        }
      }
    }
  }

  private static void editEvent(Scanner scanner) {
    List<EventInfo> events = getEventList();
    if (events.isEmpty()) {
      System.out.println("Нет мероприятий для редактирования!");
      return;
    }

    System.out.println("\n--- ВЫБЕРИТЕ МЕРОПРИЯТИЕ ДЛЯ РЕДАКТИРОВАНИЯ ---");
    for (int i = 0; i < events.size(); i++) {
      EventInfo e = events.get(i);
      System.out.printf("%d. %s (ID: %s)\n", i + 1, e.name, e.id);
    }

    System.out.print("Выберите номер мероприятия: ");
    int choice = safeParseInt(safeReadLine(scanner));
    if (choice < 1 || choice > events.size()) {
      System.out.println("[ОШИБКА] Неверный выбор!");
      return;
    }

    EventInfo selected = events.get(choice - 1);
    String eventLine = FileRepository.ticketTypes.get(selected.id);
    String[] t = eventLine.split(";");

    System.out.println("\n--- РЕДАКТИРОВАНИЕ: " + selected.name + " ---");
    System.out.println("1. Изменить цены");
    System.out.println("2. Изменить количество мест");
    System.out.println("3. Удалить Обычный тип билетов");
    System.out.println("4. Удалить VIP тип билетов");
    System.out.println("5. Удалить всё мероприятие целиком");
    System.out.println("0. Назад");
    System.out.print("Выберите действие: ");

    String editChoice = safeReadLine(scanner);

    switch (editChoice) {
      case "1":
        System.out.print("Новая цена за ОБЫЧНОЕ место (" + t[2] + " руб. -> ): ");
        int newPriceUsual = safeParseInt(safeReadLine(scanner));
        System.out.print("Новая цена за VIP место (" + t[3] + " руб. -> ): ");
        int newPriceVip = safeParseInt(safeReadLine(scanner));
        t[2] = String.valueOf(newPriceUsual);
        t[3] = String.valueOf(newPriceVip);
        FileRepository.ticketTypes.put(selected.id, String.join(";", t));
        FileRepository.saveTicketsToDisk();
        System.out.println("[УСПЕХ] Цены обновлены!");
        AuditService.log("EVENT_EDITED", "Изменены цены для " + selected.name);
        break;

      case "2":
        System.out.print("Новое количество ОБЫЧНЫХ мест (" + t[4] + " -> ): ");
        int newQuotaUsual = safeParseInt(safeReadLine(scanner));
        System.out.print("Новое количество VIP мест (" + t[5] + " -> ): ");
        int newQuotaVip = safeParseInt(safeReadLine(scanner));
        t[4] = String.valueOf(newQuotaUsual);
        t[5] = String.valueOf(newQuotaVip);
        t[6] = String.valueOf(newQuotaUsual);
        t[7] = String.valueOf(newQuotaVip);
        FileRepository.ticketTypes.put(selected.id, String.join(";", t));
        FileRepository.saveTicketsToDisk();
        System.out.println("[УСПЕХ] Количество мест обновлено!");
        AuditService.log("EVENT_EDITED", "Изменено количество мест для " + selected.name);
        break;

      case "3":
        System.out.print("Удалить Обычные билеты? (да/нет): ");
        String confirm = safeReadLine(scanner);
        if (confirm.equalsIgnoreCase("да")) {
          t[2] = "0";
          t[4] = "0";
          t[6] = "0";
          FileRepository.ticketTypes.put(selected.id, String.join(";", t));
          FileRepository.saveTicketsToDisk();
          System.out.println("[УСПЕХ] Обычные билеты удалены!");
          AuditService.log("EVENT_EDITED", "Удалены обычные билеты для " + selected.name);
        }
        break;

      case "4":
        System.out.print("Удалить VIP билеты? (да/нет): ");
        String confirmVip = safeReadLine(scanner);
        if (confirmVip.equalsIgnoreCase("да")) {
          t[3] = "0";
          t[5] = "0";
          t[7] = "0";
          FileRepository.ticketTypes.put(selected.id, String.join(";", t));
          FileRepository.saveTicketsToDisk();
          System.out.println("[УСПЕХ] VIP билеты удалены!");
          AuditService.log("EVENT_EDITED", "Удалены VIP билеты для " + selected.name);
        }
        break;

      case "5":
        System.out.print("УДАЛИТЬ ВСЁ МЕРОПРИЯТИЕ? (да/нет): ");
        String confirmDelete = safeReadLine(scanner);
        if (confirmDelete.equalsIgnoreCase("да")) {
          FileRepository.ticketTypes.remove(selected.id);
          FileRepository.saveTicketsToDisk();
          System.out.println("[УСПЕХ] Мероприятие \"" + selected.name + "\" удалено!");
          AuditService.log("EVENT_DELETED", "Удалено мероприятие " + selected.name);
        }
        break;

      default:
        System.out.println("Отмена");
    }
  }

  private static void selectEventForBooking(Scanner scanner, String userToken) {
    List<EventInfo> events = getEventList();
    if (events.isEmpty()) {
      System.out.println("Нет доступных мероприятий!");
      return;
    }

    System.out.println("\n--- ВЫБЕРИТЕ МЕРОПРИЯТИЕ ДЛЯ БРОНИРОВАНИЯ ---");
    for (int i = 0; i < events.size(); i++) {
      EventInfo e = events.get(i);
      System.out.printf("%d. %s\n", i + 1, e.toString());
    }

    System.out.print("Выберите номер мероприятия: ");
    int choice = safeParseInt(safeReadLine(scanner));
    if (choice < 1 || choice > events.size()) {
      System.out.println("[ОШИБКА] Неверный выбор!");
      return;
    }

    EventInfo selected = events.get(choice - 1);

    System.out.println("\nВыберите тип билета:");
    int typeCount = 1;
    if (selected.availableUsual > 0) {
      System.out.println(typeCount + ". Обычный (" + selected.priceUsual + " руб.) - доступно: " + selected.availableUsual);
      typeCount++;
    }
    if (selected.hasVip && selected.availableVip > 0) {
      System.out.println(typeCount + ". VIP (" + selected.priceVip + " руб.) - доступно: " + selected.availableVip);
    }
    System.out.print("Ваш выбор: ");
    int typeChoice = safeParseInt(safeReadLine(scanner));

    String ticketType = null;
    if (selected.availableUsual > 0 && typeChoice == 1) {
      ticketType = "USUAL";
    } else if (selected.hasVip && selected.availableVip > 0) {
      if ((selected.availableUsual > 0 && typeChoice == 2) || (selected.availableUsual == 0 && typeChoice == 1)) {
        ticketType = "VIP";
      }
    }

    if (ticketType == null) {
      System.out.println("[ОШИБКА] Неверный выбор типа билета!");
      return;
    }

    System.out.print("Сколько мест забронировать: ");
    int count = safeParseInt(safeReadLine(scanner));
    if (count > 0) {
      BookingService.createBooking(userToken, selected.id, ticketType, count);
    }
  }

  private static void selectEventForPurchase(Scanner scanner, String userToken, boolean isGuest, String userEmail) {
    List<EventInfo> events = getEventList();
    if (events.isEmpty()) {
      System.out.println("Нет доступных мероприятий!");
      return;
    }

    System.out.println("\n--- ВЫБЕРИТЕ МЕРОПРИЯТИЕ ДЛЯ ПОКУПКИ ---");
    for (int i = 0; i < events.size(); i++) {
      EventInfo e = events.get(i);
      System.out.printf("%d. %s\n", i + 1, e.toString());
    }

    System.out.print("Выберите номер мероприятия: ");
    int choice = safeParseInt(safeReadLine(scanner));
    if (choice < 1 || choice > events.size()) {
      System.out.println("[ОШИБКА] Неверный выбор!");
      return;
    }

    EventInfo selected = events.get(choice - 1);

    System.out.println("\nВыберите тип билета:");
    int typeCount = 1;
    if (selected.availableUsual > 0) {
      System.out.println(typeCount + ". Обычный (" + selected.priceUsual + " руб.) - доступно: " + selected.availableUsual);
      typeCount++;
    }
    if (selected.hasVip && selected.availableVip > 0) {
      System.out.println(typeCount + ". VIP (" + selected.priceVip + " руб.) - доступно: " + selected.availableVip);
    }
    System.out.print("Ваш выбор: ");
    int typeChoice = safeParseInt(safeReadLine(scanner));

    String ticketType = null;
    if (selected.availableUsual > 0 && typeChoice == 1) {
      ticketType = "USUAL";
    } else if (selected.hasVip && selected.availableVip > 0) {
      if ((selected.availableUsual > 0 && typeChoice == 2) || (selected.availableUsual == 0 && typeChoice == 1)) {
        ticketType = "VIP";
      }
    }

    if (ticketType == null) {
      System.out.println("[ОШИБКА] Неверный выбор типа билета!");
      return;
    }

    System.out.print("Сколько мест купить: ");
    int count = safeParseInt(safeReadLine(scanner));
    if (count > 0) {
      BookingService.buyTicketDirectly(userToken, userEmail, selected.id, ticketType, count, isGuest);
    }
  }

  private static List<EventInfo> getEventList() {
    List<EventInfo> events = new ArrayList<>();
    for (String ticketLine : FileRepository.ticketTypes.values()) {
      String[] t = ticketLine.split(";");
      if (t.length >= 8) {
        events.add(new EventInfo(
                t[0], t[1],
                Integer.parseInt(t[2]), Integer.parseInt(t[3]),
                Integer.parseInt(t[4]), Integer.parseInt(t[5]),
                Integer.parseInt(t[6]), Integer.parseInt(t[7])
        ));
      }
    }
    return events;
  }

  private static void showEvents() {
    System.out.println("\n--- СПИСОК МЕРОПРИЯТИЙ НА АФИШЕ ---");
    System.out.println("ID | Название | Обычный (цена/доступно) | VIP (цена/доступно)");
    for (EventInfo e : getEventList()) {
      String usualStr = (e.availableUsual > 0) ? (e.priceUsual + " руб. / " + e.availableUsual + " шт.") : "НЕТ";
      String vipStr = (e.hasVip && e.availableVip > 0) ? (e.priceVip + " руб. / " + e.availableVip + " шт.") : "НЕТ";
      System.out.printf("%s | %s | %s | %s\n", e.id, e.name, usualStr, vipStr);
    }
  }

  private static void createNewEvent(Scanner scanner, String organizerEmail) {
    System.out.print("Название концерта: ");
    String title = safeReadLine(scanner);

    System.out.println("\n--- НАСТРОЙКА ОБЫЧНЫХ МЕСТ ---");
    System.out.print("Цена за ОБЫЧНОЕ место (руб.): ");
    int priceUsual = safeParseInt(safeReadLine(scanner));
    System.out.print("Количество ОБЫЧНЫХ мест: ");
    int quotaUsual = safeParseInt(safeReadLine(scanner));

    System.out.println("\n--- НАСТРОЙКА VIP МЕСТ ---");
    System.out.print("Цена за VIP место (руб.): ");
    int priceVip = safeParseInt(safeReadLine(scanner));
    System.out.print("Количество VIP мест: ");
    int quotaVip = safeParseInt(safeReadLine(scanner));

    int nextId = FileRepository.ticketTypes.size() + 1;
    String eventId = "event_" + nextId;

    String eventLine = String.format("%s;%s;%d;%d;%d;%d;%d;%d",
            eventId, title, priceUsual, priceVip, quotaUsual, quotaVip, quotaUsual, quotaVip);

    FileRepository.addEventToDisk(eventId, eventLine);
    System.out.println("\n[СОЗДАНО] Мероприятие успешно создано!");
    System.out.println("ID: " + eventId);
    System.out.println("Название: " + title);
    System.out.println("Обычные места: " + quotaUsual + " шт. по " + priceUsual + " руб.");
    System.out.println("VIP места: " + quotaVip + " шт. по " + priceVip + " руб.");
    AuditService.log("EVENT_CREATED", "Организатор " + organizerEmail + " создал событие: " + title);
  }

  private static void showMyTickets(String userEmail, boolean isGuest) {
    System.out.println("\n--- ВАШИ КУПЛЕННЫЕ БИЛЕТЫ ---");
    int myTicketsCount = 0;

    if (isGuest) {
      for (String orderLine : FileRepository.ghostOrders) {
        String[] o = orderLine.split(";");
        if (o.length >= 5 && o[0].equalsIgnoreCase(userEmail)) {
          String ticketTypeName = "USUAL".equals(o[3]) ? "Обычный" : "VIP";
          System.out.printf("Билет %s | Событие: %s | Тип: %s | %s шт.\n", o[1], o[2], ticketTypeName, o[4]);
          myTicketsCount++;
        }
      }
    } else {
      for (String orderLine : FileRepository.orders) {
        String[] o = orderLine.split(";");
        if (o.length >= 10 && o[1].equalsIgnoreCase(userEmail) && "PAID".equals(o[6])) {
          String ticketTypeName = "USUAL".equals(o[4]) ? "Обычный" : "VIP";
          String ticketNumber = o[8];
          String checkedIn = o[9].equals("true") ? "ДА" : "НЕТ";
          System.out.printf("Билет %s | Событие: %s | Тип: %s | Кол-во: %s шт. | Прошёл: %s\n",
                  ticketNumber, o[3], ticketTypeName, o[5], checkedIn);
          myTicketsCount++;
        }
      }
    }

    if (myTicketsCount == 0) {
      System.out.println("У вас пока нет купленных билетов.");
    } else {
      System.out.println("Всего билетов: " + myTicketsCount);
    }
  }

  private static void showArchive() {
    System.out.println("\n--- АРХИВ ЗАВЕРШЕННЫХ КОНЦЕРТОВ (SOLD OUT) ---");
    try {
      java.nio.file.Path path = java.nio.file.Paths.get("archiveafisha.txt");
      if (java.nio.file.Files.exists(path)) {
        java.nio.file.Files.lines(path).forEach(line -> {
          System.out.println(line.replace(";[SOLD OUT]", ""));
        });
      } else {
        System.out.println("Архив пока пуст.");
      }
    } catch (Exception e) {
      System.out.println("Ошибка чтения архива: " + e.getMessage());
    }
  }

  private static synchronized void cleanExpired() {
    long now = System.currentTimeMillis();
    boolean changed = false;
    for (int i = 0; i < FileRepository.orders.size(); i++) {
      String[] o = FileRepository.orders.get(i).split(";");
      if (o.length >= 10 && "BOOKED".equals(o[6])) {
        long orderTime = Long.parseLong(o[7]);
        if (now - orderTime > 60000) {
          o[6] = "EXPIRED";
          FileRepository.orders.set(i, String.join(";", o));
          String eventId = o[2];
          String ticketType = o[4];
          int count = Integer.parseInt(o[5]);

          String eventLine = FileRepository.ticketTypes.get(eventId);
          if (eventLine != null) {
            String[] t = eventLine.split(";");
            if ("USUAL".equals(ticketType)) {
              int available = Integer.parseInt(t[6]);
              t[6] = String.valueOf(available + count);
            } else if ("VIP".equals(ticketType)) {
              int available = Integer.parseInt(t[7]);
              t[7] = String.valueOf(available + count);
            }
            FileRepository.ticketTypes.put(eventId, String.join(";", t));
          }
          changed = true;
          System.out.println("\n[ТАЙМЕР] Срок оплаты заказа " + o[0] + " истек. Билеты вернулись в продажу!");
          AuditService.log("BOOKING_EXPIRED", "Заказ " + o[0] + " просрочен, билеты возвращены");
        }
      }
    }
    if (changed) {
      FileRepository.saveOrdersToDisk();
      FileRepository.saveTicketsToDisk();
    }
  }

  private static void showStatistics() {
    int totalPaidTickets = 0;
    int realCheckedIn = 0;
    for (String orderLine : FileRepository.orders) {
      String[] o = orderLine.split(";");
      if (o.length >= 10 && "PAID".equals(o[6])) {
        int count = Integer.parseInt(o[5]);
        totalPaidTickets += count;
        if ("true".equals(o[9]))
          realCheckedIn += count;
      }
    }
    double percentage = totalPaidTickets == 0 ? 0.0 : ((double) realCheckedIn / totalPaidTickets) * 100;
    System.out.println("=============================================");
    System.out.println("СТАТИСТИКА ПОСЕЩАЕМОСТИ ДЛЯ ОРГАНИЗАТОРА:");
    System.out.println("Всего продано билетов: " + totalPaidTickets + " шт.");
    System.out.println("Реально прошли контроль на входе: " + realCheckedIn + " чел.");
    System.out.printf("Процент реального посещения: %.1f%%\n", percentage);
    System.out.println("=============================================");
    AuditService.log("STATISTICS", "Организатор запросил статистику: продано=" + totalPaidTickets +
            ", прошли=" + realCheckedIn + ", процент=" + percentage);
  }

  private static String safeReadLine(Scanner scanner) {
    try {
      return scanner.nextLine();
    } catch (Exception e) {
      return "";
    }
  }

  private static int safeParseInt(String str) {
    try {
      return Integer.parseInt(str.trim());
    } catch (NumberFormatException e) {
      System.out.println("[ОШИБКА] Введите корректное число!");
      return 0;
    }
  }

  static class EventInfo {
    String id;
    String name;
    int priceUsual;
    int priceVip;
    int quotaUsual;
    int quotaVip;
    int availableUsual;
    int availableVip;
    boolean hasVip;

    EventInfo(String id, String name, int priceUsual, int priceVip,
              int quotaUsual, int quotaVip, int availableUsual, int availableVip) {
      this.id = id;
      this.name = name;
      this.priceUsual = priceUsual;
      this.priceVip = priceVip;
      this.quotaUsual = quotaUsual;
      this.quotaVip = quotaVip;
      this.availableUsual = availableUsual;
      this.availableVip = availableVip;
      this.hasVip = priceVip > 0 && quotaVip > 0;
    }

    @Override
    public String toString() {
      String result = name;
      if (availableUsual > 0) {
        result += " | Обычный: " + priceUsual + " руб. (" + availableUsual + "/" + quotaUsual + ")";
      }
      if (hasVip && availableVip > 0) {
        result += " | VIP: " + priceVip + " руб. (" + availableVip + "/" + quotaVip + ")";
      }
      return result;
    }
  }
}