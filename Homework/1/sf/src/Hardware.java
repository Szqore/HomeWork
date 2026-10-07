package studio; //справочник и роль Прайс-лист

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * База данных комплектующих, видов ремонтных работ и цен.
 * Использует LinkedHashMap, чтобы элементы в выпадающих списках шли в строгом порядке.
 */
public class Hardware {
    // Справочник материнских плат: Название -> [Сокет, Тип поддерживаемой памяти ОЗУ]
    static final Map<String, String[]> MOTHERBOARDS = new LinkedHashMap<>();
    static {
        MOTHERBOARDS.put("ASUS Prime B450M",      new String[]{"AM4", "DDR4"});
        MOTHERBOARDS.put("MSI B550-A PRO",        new String[]{"AM4", "DDR4"});
        MOTHERBOARDS.put("Gigabyte X570 AORUS",   new String[]{"AM4", "DDR4"});
        MOTHERBOARDS.put("ASUS ROG STRIX Z690-A", new String[]{"LGA1700", "DDR4"});
        MOTHERBOARDS.put("MSI MAG B660M",         new String[]{"LGA1700", "DDR4"});
        MOTHERBOARDS.put("ASUS TUF Z790-PLUS",    new String[]{"LGA1700", "DDR5"});
        MOTHERBOARDS.put("Gigabyte B760M DS3H",   new String[]{"LGA1700", "DDR4"});
        MOTHERBOARDS.put("ASRock A320M-HDV",      new String[]{"AM4", "DDR4"});
    }

    // Справочник процессоров: Название процессора -> Сокет подключения
    static final Map<String, String> CPUS = new LinkedHashMap<>();
    static {
        CPUS.put("AMD Ryzen 3 3200G",    "AM4");
        CPUS.put("AMD Ryzen 5 5600X",    "AM4");
        CPUS.put("AMD Ryzen 7 5800X",    "AM4");
        CPUS.put("AMD Ryzen 9 5950X",    "AM4");
        CPUS.put("Intel Core i3-12100",  "LGA1700");
        CPUS.put("Intel Core i5-12400",  "LGA1700");
        CPUS.put("Intel Core i7-13700K", "LGA1700");
        CPUS.put("Intel Core i9-13900K", "LGA1700");
        CPUS.put("Intel Core i5-9400F",  "LGA1151");
    }

    // Справочник оперативной памяти (ОЗУ): Название планки -> Тип памяти
    static final Map<String, String> RAM = new LinkedHashMap<>();
    static {
        RAM.put("Kingston 8 ГБ DDR4",   "DDR4");
        RAM.put("Kingston 16 ГБ DDR4",  "DDR4");
        RAM.put("Corsair 32 ГБ DDR4",   "DDR4");
        RAM.put("G.Skill 16 ГБ DDR5",   "DDR5");
        RAM.put("Kingston 32 ГБ DDR5",  "DDR5");
        RAM.put("Patriot 8 ГБ DDR3",    "DDR3");
    }

    // Справочник видеокарт: Название -> Розничная цена в рублях
    static final Map<String, Integer> GPUS = new LinkedHashMap<>();
    static {
        GPUS.put("Встроенная (не менять)", 0);
        GPUS.put("NVIDIA GTX 1650", 18000);
        GPUS.put("NVIDIA RTX 3060", 32000);
        GPUS.put("NVIDIA RTX 4060", 42000);
        GPUS.put("AMD Radeon RX 6600", 25000);
        GPUS.put("AMD Radeon RX 7800 XT", 60000);
    }

    // Список доступных типов работ для выпадающего списка
    static final String[] WORK_TYPES = {
            "Не менять",
            "Заменить процессор",
            "Заменить материнскую плату",
            "Заменить оперативную память",
            "Заменить видеокарту",
            "Заменить термопасту",
            "Почистить от пыли"
    };

    // Цены на выполнение конкретных технических услуг (только стоимость работы)
    static final Map<String, Integer> WORK_PRICES = new LinkedHashMap<>();
    static {
        WORK_PRICES.put("Не менять", 0);
        WORK_PRICES.put("Заменить процессор", 1500);
        WORK_PRICES.put("Заменить материнскую плату", 2500);
        WORK_PRICES.put("Заменить оперативную память", 500);
        WORK_PRICES.put("Заменить видеокарту", 800);
        WORK_PRICES.put("Заменить термопасту", 700);
        WORK_PRICES.put("Почистить от пыли", 800);
    }

    // Единый глобальный прайс-лист на комплектующие для расчета стоимости апгрейда
    static final Map<String, Integer> PRICES = new LinkedHashMap<>();
    static {
        // Цены на материнские платы
        PRICES.put("ASUS Prime B450M", 6500);
        PRICES.put("MSI B550-A PRO", 11000);
        PRICES.put("Gigabyte X570 AORUS", 18000);
        PRICES.put("ASUS ROG STRIX Z690-A", 25000);
        PRICES.put("MSI MAG B660M", 12000);
        PRICES.put("ASUS TUF Z790-PLUS", 28000);
        PRICES.put("Gigabyte B760M DS3H", 11000);
        PRICES.put("ASRock A320M-HDV", 4500);

        // Цены на процессоры
        PRICES.put("AMD Ryzen 3 3200G", 5500);
        PRICES.put("AMD Ryzen 5 5600X", 12000);
        PRICES.put("AMD Ryzen 7 5800X", 18000);
        PRICES.put("AMD Ryzen 9 5950X", 38000);
        PRICES.put("Intel Core i3-12100", 8000);
        PRICES.put("Intel Core i5-12400", 13000);
        PRICES.put("Intel Core i7-13700K", 32000);
        PRICES.put("Intel Core i9-13900K", 55000);
        PRICES.put("Intel Core i5-9400F", 6000);

        // Цены на оперативную память
        PRICES.put("Kingston 8 ГБ DDR4", 2200);
        PRICES.put("Kingston 16 ГБ DDR4", 4200);
        PRICES.put("Corsair 32 ГБ DDR4", 8500);
        PRICES.put("G.Skill 16 ГБ DDR5", 7000);
        PRICES.put("Kingston 32 ГБ DDR5", 13000);
        PRICES.put("Patriot 8 ГБ DDR3", 1500);

        // Цены на видеокарты
        PRICES.put("Встроенная (не менять)", 0);
        PRICES.put("NVIDIA GTX 1650", 18000);
        PRICES.put("NVIDIA RTX 3060", 32000);
        PRICES.put("NVIDIA RTX 4060", 42000);
        PRICES.put("AMD Radeon RX 6600", 25000);
        PRICES.put("AMD Radeon RX 7800 XT", 60000);
    }
}
