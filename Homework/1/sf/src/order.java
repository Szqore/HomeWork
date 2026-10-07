package studio; // роль  Бланк заказа

/**
 * Класс-модель (Сущность) одной ремонтной заявки.
 * Хранит всю заполненную информацию о клиенте, его устройстве и типе ремонта.
 */
public class order {
    // Уникальный номер (ID) заявки
    int id;

    // Текстовые данные клиента и устройства(BACKEND)
    String name;    // ФИО клиента
    String phone;   // Номер телефона
    String model;   // Модель компьютера или ноутбука
    String issue;   // Основной тип проблемы (выбирается из списка)
    String urgency; // Срочность выполнения (влияет на наценку)
    String desc;    // Подробное текстовое описание проблемы или лог апгрейда
    String date;    // Дата и время создания заявки в формате дд.мм.гггг чч:мм

    // Пустой конструктор по умолчанию (необходим для парсера JSON)
    public order() {}

    // Конструктор для быстрого создания заполненного объекта заявки
    public order(int id, String name, String phone, String model,
                 String issue, String urgency, String desc, String date) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.model = model;
        this.issue = issue;
        this.urgency = urgency;
        this.desc = desc;
        this.date = date;
    }
}
