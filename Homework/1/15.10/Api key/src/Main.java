import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * ============================================================
 *  КЛАСС MAIN
 * ============================================================
 *  Точка входа приложения «Погода».
 *
 *  Здесь только:
 *   - создание главного окна
 *   - поле для ввода города
 *   - три кнопки: "Узнать погоду" / "Кастом" / "Сервис"
 *   - вывод результата
 *
 *  Вся логика погоды   — в WeatherApi.java
 *  Вся логика темы/RGB — в Theme.java
 * ============================================================
 */
public class Main {

    // ---------- Ссылки на компоненты окна ----------
    // Вынесены в поля класса, чтобы методы applyTheme()
    // и openServiceChooser() могли к ним обращаться.
    private static JFrame frame;
    private static JPanel mainPanel;      // главная панель окна
    private static JPanel topPanel;       // верхняя панель (поле + кнопки)
    private static JLabel cityLabel;      // метка "Город:"
    private static JTextField cityField;  // поле ввода города
    private static JButton searchBtn;     // кнопка "Узнать погоду"
    private static JButton customBtn;     // кнопка "Кастом"
    private static JButton serviceBtn;    // кнопка "Сервис"
    private static JTextArea result;      // область результата
    private static JScrollPane scroll;    // скролл вокруг result

    // Текущий выбранный сервис погоды.
    // По умолчанию — OpenWeatherMap.
    private static WeatherApi.Service currentService = WeatherApi.Service.OPEN_WEATHER;

    /**
     * Точка входа в программу.
     */
    public static void main(String[] args) {
        // Ставим Metal L&F — единственный, который не навязывает
        // свои цвета кнопкам/полям и позволяет красить их вручную.
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            UIManager.put("Button.gradient", null);
            UIManager.put("Button.select", new Color(0, 0, 0, 0));
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        } catch (Exception ignored) {}

        // Swing должен запускаться в отдельном UI-потоке
        SwingUtilities.invokeLater(Main::createWindow);
    }

    /**
     * Создаёт и показывает главное окно.
     */
    private static void createWindow() {
        frame = new JFrame("Погода");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 440);
        frame.setLocationRelativeTo(null); // центрируем окно

        // Главная панель — BorderLayout: сверху topPanel, по центру scroll
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Верхняя панель — слева поле с меткой, справа кнопки
        topPanel = new JPanel(new BorderLayout(10, 10));

        // ------- Метка "Город:" -------
        cityLabel = new JLabel("Город:");
        cityLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));

        // ------- Поле ввода города -------
        // Без setPreferredSize — пусть растягивается под окно
        cityField = new JTextField();
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 15));

        // ------- Левая группа: [Город:] [поле] -------
        JPanel left = new JPanel(new BorderLayout(8, 0));
        left.setOpaque(false);
        left.add(cityLabel, BorderLayout.WEST);
        left.add(cityField, BorderLayout.CENTER);

        // ------- Кнопки -------
        searchBtn = new JButton("Узнать погоду");
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        searchBtn.setPreferredSize(new Dimension(150, 32));

        customBtn = new JButton("Кастом");
        customBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        customBtn.setPreferredSize(new Dimension(100, 32));

        serviceBtn = new JButton("Сервис");
        serviceBtn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        serviceBtn.setPreferredSize(new Dimension(100, 32));

        // ------- Правая группа: [Узнать погоду] [Кастом] [Сервис] -------
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(searchBtn);
        right.add(customBtn);
        right.add(serviceBtn);

        // Собираем верхнюю панель
        topPanel.add(left, BorderLayout.CENTER);
        topPanel.add(right, BorderLayout.EAST);

        // ------- Область результата -------
        result = new JTextArea();
        result.setEditable(false);  // только для чтения
        result.setFont(new Font("SansSerif", Font.PLAIN, 15));
        result.setLineWrap(true);       // перенос длинных строк
        result.setWrapStyleWord(true);  // перенос по словам
        result.setBorder(new EmptyBorder(10, 10, 10, 10));

        scroll = new JScrollPane(result);

        // ------- Собираем окно -------
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);
        frame.add(mainPanel);

        // ------- Применяем светлую тему и показываем окно -------
        Theme.useLight();
        applyTheme();
        frame.setVisible(true);

        // ======== ОБРАБОТЧИКИ КНОПОК ========

        // Кнопка "Кастом" — открывает диалог настройки темы из Theme.java
        customBtn.addActionListener(e ->
                Theme.openCustomDialog(frame, Main::applyTheme));

        // Кнопка "Сервис" — открывает окно выбора сервиса
        serviceBtn.addActionListener(e -> openServiceChooser());

        // Кнопка "Узнать погоду" (и Enter в поле города)
        Runnable doSearch = () -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                result.setText("Введите название города.");
                return;
            }

            // Пока идёт запрос — показываем "Загрузка..."
            result.setText("Загрузка (" + currentService.getTitle() + ")…");
            searchBtn.setEnabled(false);

            // Запрос делаем в отдельном потоке,
            // чтобы окно не подвисло.
            new Thread(() -> {
                try {
                    // Спросить погоду у WeatherApi,
                    // передав и сервис, и город
                    String text = WeatherApi.getWeather(currentService, city);

                    // Обновляем UI из UI-потока
                    SwingUtilities.invokeLater(() -> result.setText(text));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            result.setText("Ошибка: " + ex.getClass().getSimpleName()
                                    + (ex.getMessage() != null ? " — " + ex.getMessage() : "")));
                } finally {
                    // Кнопку включаем обратно
                    SwingUtilities.invokeLater(() -> searchBtn.setEnabled(true));
                }
            }).start();
        };

        searchBtn.addActionListener(e -> doSearch.run());
        cityField.addActionListener(e -> doSearch.run()); // Enter в поле тоже ищет
    }

    /**
     * Открывает маленькое модальное окно выбора сервиса.
     * Внутри две кнопки: OpenWeatherMap и WeatherAPI.
     */
    private static void openServiceChooser() {
        JDialog dialog = new JDialog(frame, "Выбор сервиса", true);
        dialog.setSize(360, 160);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(Theme.bg);

        // Заголовок вверху
        JLabel title = new JLabel("Выберите сервис погоды:", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(Theme.text);
        title.setBorder(new EmptyBorder(15, 10, 5, 10));

        // Панель с двумя кнопками
        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 10));
        buttons.setBackground(Theme.bg);
        buttons.setBorder(new EmptyBorder(10, 15, 15, 15));

        JButton owmBtn  = new JButton("OpenWeatherMap");
        JButton wapiBtn = new JButton("WeatherAPI");
        owmBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        wapiBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        Theme.styleButton(owmBtn,  Theme.accent, Color.WHITE);
        Theme.styleButton(wapiBtn, Theme.accent, Color.WHITE);

        // Пометим галочкой тот сервис, который выбран сейчас
        if (currentService == WeatherApi.Service.OPEN_WEATHER) {
            owmBtn.setText("✓ OpenWeatherMap");
        } else {
            wapiBtn.setText("✓ WeatherAPI");
        }

        // Обработчики выбора
        owmBtn.addActionListener(e -> {
            currentService = WeatherApi.Service.OPEN_WEATHER;
            dialog.dispose();
        });
        wapiBtn.addActionListener(e -> {
            currentService = WeatherApi.Service.WEATHER_API;
            dialog.dispose();
        });

        buttons.add(owmBtn);
        buttons.add(wapiBtn);

        dialog.add(title, BorderLayout.NORTH);
        dialog.add(buttons, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    /**
     * Перекрасить всё окно текущими цветами темы.
     * Вызывается:
     *  - при старте
     *  - после изменения темы (пресет, кастом, RGB)
     */
    private static void applyTheme() {
        // Основная перекраска — в Theme.apply(...)
        // serviceLabel/serviceBox больше не нужны, поэтому null.
        Theme.apply(frame, mainPanel, topPanel,
                cityLabel, cityField,
                searchBtn, customBtn,
                result, scroll,
                null, null);

        // Кнопку "Сервис" тоже перекрашиваем как обычные кнопки
        if (serviceBtn != null) {
            Theme.styleButton(serviceBtn, Theme.field, Theme.text);
        }
    }
}