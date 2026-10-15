import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  КЛАСС WeatherApi
 * ============================================================
 *  Работает с двумя сервисами погоды:
 *    1) OpenWeatherMap  (OWM)   — https://openweathermap.org/
 *    2) WeatherAPI.com  (WAPI)  — https://www.weatherapi.com/
 *
 *  Какой использовать — определяется enum Service,
 *  который выбирает пользователь в окне Main.
 *
 *  Здесь НЕТ ни одной кнопки и ни одной темы — только:
 *   - HTTP-запросы
 *   - парсинг JSON
 *   - сборка текста для JTextArea
 * ============================================================
 */
public class WeatherApi {

    /**
     * Какие сервисы поддерживаются.
     * Enum автоматически используется как элементы JComboBox.
     */
    public enum Service {
        OPEN_WEATHER("OpenWeatherMap"),
        WEATHER_API("WeatherAPI");

        private final String title;
        Service(String title) { this.title = title; }
        public String getTitle() { return title; }

        @Override
        public String toString() { return title; } // чтобы JComboBox показывал название
    }

    // ============ КЛЮЧИ ============
    // OpenWeatherMap — ключ из личного кабинета OWM.
    private static final String OWM_KEY  = "f551e615e38b23a3f94881573cb15b17";

    // WeatherAPI — ключ из личного кабинета WeatherAPI.
    private static final String WAPI_KEY = "dad92b5999fb41e6b6a85024261510";
    // ===============================

    // URL сервисов
    private static final String OWM_URL  = "https://api.openweathermap.org/data/2.5/weather";
    private static final String WAPI_URL = "https://api.weatherapi.com/v1/current.json";

    // Один общий HTTP-клиент на всю программу
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) // максимум 10 секунд на соединение
            .build();

    /**
     * Главный метод: получить погоду по названию города.
     * В зависимости от выбранного сервиса вызывает один из двух методов.
     *
     * @param service какой сервис использовать
     * @param city    название города
     * @return готовый текст для отображения в окне
     */
    public static String getWeather(Service service, String city) throws Exception {
        return (service == Service.OPEN_WEATHER)
                ? fetchOpenWeather(city)
                : fetchWeatherApi(city);
    }

    // ================================================================
    //                       OpenWeatherMap
    // ================================================================
    private static String fetchOpenWeather(String city) throws Exception {
        // 1. Кодируем город (русские буквы -> %D0%...)
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);

        // 2. Собираем URL: город + ключ + метрические единицы + русский язык
        String url = OWM_URL
                + "?q=" + encoded
                + "&appid=" + OWM_KEY
                + "&units=metric"
                + "&lang=ru";

        // 3. Запрос
        System.out.println(">>> [OWM] Запрос: " + url);
        String body = httpGet(url);
        System.out.println(">>> [OWM] Ответ: " + body);

        // 4. Ошибка сервиса?
        if (body.contains("\"cod\"") && !body.contains("\"cod\":200")) {
            String msg = extractString(body, "\"message\"");
            return "OpenWeatherMap: ошибка.\n" + (msg != null ? msg : body);
        }

        // 5. Вытаскиваем поля
        String name    = extractString(body, "\"name\"");
        String country = extractString(body, "\"country\"");
        Double temp     = extractDouble(body, "\"temp\"");
        Double feels    = extractDouble(body, "\"feels_like\"");
        Double humidity = extractDouble(body, "\"humidity\"");
        Double pressure = extractDouble(body, "\"pressure\"");
        Double wind     = extractDouble(body, "\"speed\"");
        String desc     = extractString(body, "\"description\"");

        if (temp == null) return "OpenWeatherMap: сервер не вернул температуру.\n" + body;

        // 6. Собираем текст для окна
        StringBuilder sb = new StringBuilder();
        sb.append("Сервис: OpenWeatherMap\n");
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

    // ================================================================
    //                          WeatherAPI
    // ================================================================
    private static String fetchWeatherApi(String city) throws Exception {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = WAPI_URL
                + "?key=" + WAPI_KEY
                + "&q=" + encoded
                + "&lang=ru";

        System.out.println(">>> [WAPI] Запрос: " + url);
        String body = httpGet(url);
        System.out.println(">>> [WAPI] Ответ: " + body);

        if (body.contains("\"error\"")) {
            String msg = extractString(body, "\"message\"");
            return "WeatherAPI: ошибка.\n" + (msg != null ? msg : body);
        }

        String name    = extractString(body, "\"name\"");
        String country = extractString(body, "\"country\"");
        Double temp     = extractDouble(body, "\"temp_c\"");
        Double feels    = extractDouble(body, "\"feelslike_c\"");
        Double humidity = extractDouble(body, "\"humidity\"");
        Double pressure = extractDouble(body, "\"pressure_mb\"");
        Double wind     = extractDouble(body, "\"wind_kph\"");
        String desc     = extractString(body, "\"text\"");

        if (temp == null) return "WeatherAPI: сервер не вернул температуру.\n" + body;

        StringBuilder sb = new StringBuilder();
        sb.append("Сервис: WeatherAPI\n");
        sb.append("Город: ").append(name != null ? name : city);
        if (country != null) sb.append(", ").append(country);
        sb.append("\n\n");
        sb.append("Погода: ").append(desc != null ? capitalize(desc) : "—").append("\n");
        sb.append("Температура: ").append(temp).append(" °C\n");
        sb.append("Ощущается как: ").append(feels != null ? feels : "—").append(" °C\n");
        sb.append("Влажность: ").append(humidity != null ? humidity.intValue() : "—").append(" %\n");
        sb.append("Давление: ").append(pressure != null ? pressure.intValue() : "—").append(" гПа\n");
        sb.append("Ветер: ").append(wind != null ? wind : "—").append(" км/ч\n");
        return sb.toString();
    }

    // ================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==================

    /** Отправить GET-запрос и вернуть тело ответа строкой. */
    private static String httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10)) // таймаут на сам запрос
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println(">>> HTTP статус: " + response.statusCode());
        return response.body();
    }

    /** Достать из JSON число по имени поля (например "\"temp\""). */
    private static Double extractDouble(String json, String key) {
        Pattern p = Pattern.compile(Pattern.quote(key) + "\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /** Достать из JSON строку по имени поля (например "\"name\""). */
    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile(Pattern.quote(key) + "\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Первая буква — заглавная: "ясно" → "Ясно". */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}