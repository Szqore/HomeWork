import javax.swing.*; // Рисуем интерфейс
import javax.swing.border.EmptyBorder; //Чтобы текст не прилипал к краям окна
import java.awt.*; //Оформление
import java.net.URI; // Превращаем строку URL в URI, понятный Java
import java.net.URLEncoder; //Кодирование русских букв в URL
import java.net.http.HttpClient; // Сам «клиент» для запросов в интернет
import java.net.http.HttpRequest; // Описание одного запроса
import java.net.http.HttpResponse; // Ответ от сервера
import java.nio.charset.StandardCharsets; // Кодировки (UTF-8 и др.)
import java.time.Duration; // Промежуток времени
import java.util.regex.Matcher; //Поиск по регулярному выражению
import java.util.regex.Pattern; //Само регулярное выражение


public class Main {

    // ============  КЛЮЧ OpenWeatherMap ============
    private static final String API_KEY = "f551e615e38b23a3f94881573cb15b17";
    // ==================================================

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) { //точка входа
        // Metal L&F — чтобы кнопки норм были
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            UIManager.put("Button.gradient", null);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        // Окно
        JFrame frame = new JFrame("Погода");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 420);
        frame.setLocationRelativeTo(null);

        // Главная панель
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Верх: метка + поле + кнопка
        JPanel top = new JPanel(new BorderLayout(8, 0));

        JLabel label = new JLabel("Город:");
        label.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JTextField cityField = new JTextField();
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JButton searchBtn = new JButton("Узнать погоду");
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Левая часть — метка + поле (поле растягивается)
        JPanel left = new JPanel(new BorderLayout(8, 0));
        left.add(label, BorderLayout.WEST);
        left.add(cityField, BorderLayout.CENTER);

        top.add(left, BorderLayout.CENTER);
        top.add(searchBtn, BorderLayout.EAST);

        // результат
        JTextArea result = new JTextArea();
        result.setEditable(false);
        result.setFont(new Font("SansSerif", Font.PLAIN, 15));
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(result);
        //тут собираем все
        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        frame.add(panel);
        frame.setVisible(true);

        // Логика поиска
        Runnable doSearch = () -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                result.setText("Введите название города.");
                return;
            }
            result.setText("Загрузка...");
            searchBtn.setEnabled(false);

            // Запрос в отдельном потоке, чтобы окно не подвисало
            new Thread(() -> {
                try {
                    String text = getWeather(city);
                    SwingUtilities.invokeLater(() -> result.setText(text));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            result.setText("Ошибка: " + ex.getMessage()));
                } finally {
                    SwingUtilities.invokeLater(() -> searchBtn.setEnabled(true));
                }
            }).start();
        };

        searchBtn.addActionListener(e -> doSearch.run());
        cityField.addActionListener(e -> doSearch.run());
    }

    // ================= использование ключа =================
    private static String getWeather(String city) throws Exception {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=" + encoded
                + "&appid=" + API_KEY
                + "&units=metric"
                + "&lang=ru";
        // получил ответ севера
        String body = httpGet(url);
        System.out.println("Ответ сервера: " + body);

        // Ошибка от сервера(проверка не вернул ли обпроатен)
        if (body.contains("\"cod\"") && !body.contains("\"cod\":200")) {
            String msg = extractString(body, "\"message\"");
            return "Не удалось получить погоду.\n" + (msg != null ? msg : body);
        }
        //Вытаскиваем все нужные поля из JSON
        String name    = extractString(body, "\"name\"");
        String country = extractString(body, "\"country\"");
        Double temp     = extractDouble(body, "\"temp\"");
        Double feels    = extractDouble(body, "\"feels_like\"");
        Double humidity = extractDouble(body, "\"humidity\"");
        Double pressure = extractDouble(body, "\"pressure\"");
        Double wind     = extractDouble(body, "\"speed\"");
        String desc     = extractString(body, "\"description\"");
        //есть ли вообще температура
        if (temp == null) return "Сервер не вернул температуру.";
//Собираем текст для окна
        StringBuilder sb = new StringBuilder();
        sb.append("Город: ").append(name != null ? name : city);
        if (country != null) sb.append(", ").append(country);
        sb.append("\n\n");
        sb.append("Погода: ").append(desc != null ? capitalize(desc) : "—").append("\n");
        sb.append("Температура: ").append(temp).append(" °C\n");
        sb.append("Ощущается как: ").append(feels != null ? feels : "—").append(" °C\n");
        sb.append("Влажность: ").append(humidity != null ? humidity.intValue() : "—").append(" %\n");
        sb.append("Давление: ").append(pressure != null ? pressure.intValue() : "—").append(" гПа\n");
        sb.append("Ветер: ").append(wind != null ? wind : "—").append(" м/с\n");
        return sb.toString();
    }
//как реально идёт запрос
    private static String httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.body();
    }
//достать число из JSON
    private static Double extractDouble(String json, String key) {
        Pattern p = Pattern.compile(Pattern.quote(key) + "\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }
//достать строку из JSON
    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile(Pattern.quote(key) + "\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
//первая буква заглавная
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}