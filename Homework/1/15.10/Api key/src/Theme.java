import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * ============================================================
 *  КЛАСС THEME
 * ============================================================
 *  Всё, что связано с внешним видом приложения:
 *   - цвета светлой темы (по умолчанию)
 *   - пресеты (готовые темы)
 *   - диалог "Кастомная тема"
 *   - RGB-перелив (плавная смена оттенков)
 *
 *  Меняешь цвета здесь — они применяются во всём окне
 *  через метод apply(...).
 * ============================================================
 */
public class Theme {

    // ============ ЦВЕТА СВЕТЛОЙ ТЕМЫ (по умолчанию) ============
    public static final Color LIGHT_BG     = new Color(245, 245, 245); // фон окна
    public static final Color LIGHT_PANEL  = new Color(255, 255, 255); // фон результата
    public static final Color LIGHT_FIELD  = new Color(255, 255, 255); // фон полей
    public static final Color LIGHT_TEXT   = new Color(30, 30, 30);    // текст
    public static final Color LIGHT_BORDER = new Color(180, 180, 180); // рамки
    public static final Color LIGHT_ACCENT = new Color(0, 122, 204);   // акцентная кнопка

    // ============ ТЕКУЩИЕ ЦВЕТА ============
    // Эти поля всегда отражают то, что сейчас на экране.
    // Их переписывают useLight(), useCustom(), startRgb().
    public static Color bg     = LIGHT_BG;
    public static Color panel  = LIGHT_PANEL;
    public static Color field  = LIGHT_FIELD;
    public static Color text   = LIGHT_TEXT;
    public static Color border = LIGHT_BORDER;
    public static Color accent = LIGHT_ACCENT;

    // ============ RGB-ПЕРЕЛИВ ============
    private static volatile boolean rgbRunning = false; // работает ли анимация
    private static Thread rgbThread;                    // поток анимации

    // ============ ПРЕСЕТЫ ============
    // PRESET_NAMES — то, что показывается в списке
    // PRESET_COLORS — цвета {фон, кнопка, текст} для каждого пресета
    public static final String[] PRESET_NAMES = {
            "— Выберите тему —",
            "Волна", "Закат", "Лес", "Океан", "Роза",
            "Космос", "Ретро", "Лаванда", "Уголь"
    };
    public static final Color[][] PRESET_COLORS = {
            null, // для строки "— Выберите тему —" цветов нет
            { new Color(0x1B,0x3A,0x5C), new Color(0x2E,0x86,0xAB), new Color(0xEA,0xF6,0xFF) }, // Волна
            { new Color(0x33,0x18,0x28), new Color(0xE9,0x6C,0x2A), new Color(0xFF,0xE8,0xCC) }, // Закат
            { new Color(0x1E,0x2A,0x1E), new Color(0x4C,0xAF,0x50), new Color(0xE8,0xF5,0xE9) }, // Лес
            { new Color(0x0A,0x1F,0x2B), new Color(0x14,0x8A,0xB8), new Color(0xDD,0xF3,0xFF) }, // Океан
            { new Color(0x3A,0x1B,0x2E), new Color(0xE9,0x4F,0x8B), new Color(0xFF,0xE4,0xEF) }, // Роза
            { new Color(0x0F,0x0B,0x1F), new Color(0x8E,0x44,0xAD), new Color(0xF3,0xE5,0xFF) }, // Космос
            { new Color(0x2B,0x22,0x18), new Color(0xC0,0x8A,0x35), new Color(0xFF,0xF3,0xD9) }, // Ретро
            { new Color(0x2A,0x1F,0x3D), new Color(0x9A,0x7C,0xD4), new Color(0xF3,0xEC,0xFF) }, // Лаванда
            { new Color(0x1C,0x1C,0x1C), new Color(0x44,0x44,0x44), new Color(0xEE,0xEE,0xEE) }  // Уголь
    };

    /** Сбросить цвета на светлую тему. */
    public static void useLight() {
        bg=LIGHT_BG; panel=LIGHT_PANEL; field=LIGHT_FIELD;
        text=LIGHT_TEXT; border=LIGHT_BORDER; accent=LIGHT_ACCENT;
    }

    /**
     * Установить кастомные цвета.
     * Из переданных bg/accent/text автоматически выводятся
     * panel/field/border — они чуть светлее/темнее фона.
     */
    public static void useCustom(Color b, Color a, Color t) {
        bg=b; accent=a; text=t;
        panel=shift(b,8);
        field=shift(b,20);
        border=shift(b,40);
    }

    /** Осветлить (d>0) или затемнить (d<0) цвет на d единиц. */
    private static Color shift(Color c, int d) {
        return new Color(clamp(c.getRed()+d), clamp(c.getGreen()+d), clamp(c.getBlue()+d));
    }

    /** Ограничить значение диапазоном 0..255. */
    private static int clamp(int v){ return Math.max(0, Math.min(255, v)); }

    /**
     * Применить текущие цвета ко всем компонентам главного окна.
     * Вызывается при старте и после любого изменения темы.
     */
    public static void apply(JFrame frame, JPanel mainPanel, JPanel topPanel,
                             JLabel cityLabel, JTextField cityField,
                             JButton searchBtn, JButton customBtn,
                             JTextArea result, JScrollPane scroll,
                             JLabel serviceLabel, JComboBox<?> serviceBox) {

        // Фон окна и панелей
        frame.getContentPane().setBackground(bg);
        mainPanel.setBackground(bg);
        topPanel.setBackground(bg);

        // Метка "Город:"
        cityLabel.setForeground(text);
        cityLabel.setOpaque(false);

        // Метка "Сервис:" (может быть null, если её нет)
        if (serviceLabel != null) {
            serviceLabel.setForeground(text);
            serviceLabel.setOpaque(false);
        }

        // Поле города
        cityField.setBackground(field);
        cityField.setForeground(text);
        cityField.setCaretColor(text);
        cityField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(5, 8, 5, 8)));

        // Выпадающий список сервисов (может быть null)
        if (serviceBox != null) {
            serviceBox.setBackground(field);
            serviceBox.setForeground(text);
        }

        // Кнопки — рисуем вручную
        styleButton(searchBtn, accent, Color.WHITE);
        styleButton(customBtn, field, text);

        // Область результата
        result.setBackground(panel);
        result.setForeground(text);
        result.setCaretColor(text);

        // Скролл
        scroll.setBorder(BorderFactory.createLineBorder(border));
        scroll.getViewport().setBackground(panel);

        frame.repaint();
    }

    /**
     * Ручная отрисовка кнопки.
     * Нужна, чтобы системный L&F не рисовал поверх наших цветов
     * светлые полоски и серые градиенты.
     */
    public static void styleButton(JButton btn, Color bgColor, Color fgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setRolloverEnabled(false);
        btn.setMargin(new Insets(6,14,6,14));

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Фон кнопки
                g2.setColor(btn.getBackground());
                g2.fillRect(0,0,c.getWidth(),c.getHeight());
                // Рамка
                g2.setColor(Theme.border);
                g2.drawRect(0,0,c.getWidth()-1,c.getHeight()-1);
                g2.dispose();
                // Текст рисует супер-класс
                super.paint(g,c);
            }
        });
    }

    // =========================================================
    //                  RGB-ПЕРЕЛИВ
    // =========================================================
    /**
     * Запустить плавную смену цвета окна.
     * В отдельном потоке раз в speedMs миллисекунд сдвигаем оттенок.
     *
     * @param parent  окно (не используется, оставлено для совместимости)
     * @param onApply функция из Main, перерисовывающая окно
     * @param speedMs пауза между кадрами анимации
     */
    public static void startRgb(JFrame parent, Runnable onApply, int speedMs) {
        stopRgb(); // на случай, если что-то уже крутится
        rgbRunning = true;

        rgbThread = new Thread(() -> {
            float hue = 0f;
            while (rgbRunning) {
                final float h = hue;

                bg     = Color.getHSBColor(h, 0.55f, 0.95f);                // фон
                accent = Color.getHSBColor((h + 0.5f) % 1f, 0.85f, 0.75f);  // кнопка
                text   = new Color(20, 20, 20);                             // текст
                panel  = shift(bg, 12);
                field  = shift(bg, 30);
                border = shift(bg, 60);

                // Перерисовка в UI-потоке
                SwingUtilities.invokeLater(onApply);

                hue += 0.006f;
                if (hue > 1f) hue -= 1f;

                try { Thread.sleep(speedMs); }
                catch (InterruptedException e) { return; }
            }
        }, "rgb-anim");
        rgbThread.setDaemon(true);
        rgbThread.start();
    }

    /** Остановить RGB-перелив. */
    public static void stopRgb() {
        rgbRunning = false;
        if (rgbThread != null) {
            rgbThread.interrupt();
            rgbThread = null;
        }
    }

    public static boolean isRgbRunning() { return rgbRunning; }

    // =========================================================
    //            ДИАЛОГ "КАСТОМНАЯ ТЕМА"
    // =========================================================
    /**
     * Открывает модальное окно с тремя вкладками:
     *   1. Цвета   — выбор фона/кнопки/текста через палитру
     *   2. Пресеты — список готовых тем
     *   3. RGB     — плавный перелив
     *
     * @param parent  окно, к которому привязан диалог
     * @param onApply функция из Main, которая перерисует главное окно
     */
    public static void openCustomDialog(JFrame parent, Runnable onApply) {
        JDialog dialog = new JDialog(parent, "Кастомная тема", true);
        dialog.setSize(560, 460);
        dialog.setLocationRelativeTo(parent);
        dialog.getContentPane().setBackground(bg);

        // Массив текущего выбора: [0]=фон, [1]=кнопка, [2]=текст
        final Color[] chosen = { bg, accent, text };

        // ---------- Вкладка "Цвета" ----------
        JPanel colorsTab = new JPanel(new GridLayout(3,1,8,8));
        colorsTab.setBorder(new EmptyBorder(15,15,15,15));
        colorsTab.setBackground(bg);
        colorsTab.add(makeColorRow("Фон",    chosen, 0, dialog, onApply));
        colorsTab.add(makeColorRow("Кнопка", chosen, 1, dialog, onApply));
        colorsTab.add(makeColorRow("Текст",  chosen, 2, dialog, onApply));

        // ---------- Вкладка "Пресеты" ----------
        JPanel presetsTab = new JPanel(new BorderLayout(10,10));
        presetsTab.setBorder(new EmptyBorder(15,15,15,15));
        presetsTab.setBackground(bg);

        JComboBox<String> presetBox = new JComboBox<>(PRESET_NAMES);
        presetBox.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel previewPanel = new JPanel(new GridLayout(1,3,8,8));
        previewPanel.setBackground(bg);
        JPanel pBg  = makePreview("Фон");
        JPanel pBtn = makePreview("Кнопка");
        JPanel pTxt = makePreview("Текст");
        previewPanel.add(pBg); previewPanel.add(pBtn); previewPanel.add(pTxt);

        // Выбор пресета сразу применяется к окну
        presetBox.addActionListener(e -> {
            int idx = presetBox.getSelectedIndex();
            if (idx > 0) {
                Color[] pc = PRESET_COLORS[idx];
                chosen[0]=pc[0]; chosen[1]=pc[1]; chosen[2]=pc[2];
                pBg.setBackground(pc[0]);
                pBtn.setBackground(pc[1]);
                pTxt.setBackground(pc[2]);
                previewPanel.repaint();
                useCustom(pc[0], pc[1], pc[2]);
                onApply.run();
            }
        });

        presetsTab.add(presetBox, BorderLayout.NORTH);
        presetsTab.add(previewPanel, BorderLayout.CENTER);

        // ---------- Вкладка "RGB" ----------
        JPanel rgbTab = new JPanel(new BorderLayout(10,10));
        rgbTab.setBorder(new EmptyBorder(15,15,15,15));
        rgbTab.setBackground(bg);

        JLabel info = new JLabel("<html>Плавный перелив всей палитры окна.<br>" +
                "Скорость можно менять.</html>");
        info.setForeground(text);
        info.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // Ползунок скорости
        JSlider speed = new JSlider(10, 200, 50);
        speed.setBackground(bg);
        JLabel speedLbl = new JLabel("Скорость: средняя");
        speedLbl.setForeground(text);
        speedLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));

        speed.addChangeListener(e -> {
            int v = speed.getValue();
            String label = v < 30 ? "быстрая"
                    : v < 80 ? "средняя"
                      : v < 130 ? "медленная"
                        : "очень медленная";
            speedLbl.setText("Скорость: " + label);
        });

        // Кнопки управления переливом
        JButton startBtn = new JButton("Включить RGB-перелив");
        JButton stopBtn  = new JButton("Стоп");
        styleButton(startBtn, accent, Color.WHITE);
        styleButton(stopBtn, field, text);

        startBtn.addActionListener(e -> startRgb(parent, onApply, speed.getValue()));
        stopBtn.addActionListener(e -> stopRgb());

        JPanel rgbCenter = new JPanel();
        rgbCenter.setLayout(new BoxLayout(rgbCenter, BoxLayout.Y_AXIS));
        rgbCenter.setBackground(bg);
        rgbCenter.add(info);
        rgbCenter.add(Box.createVerticalStrut(15));
        rgbCenter.add(speedLbl);
        rgbCenter.add(speed);
        rgbCenter.add(Box.createVerticalStrut(20));
        rgbCenter.add(startBtn);
        rgbCenter.add(Box.createVerticalStrut(8));
        rgbCenter.add(stopBtn);

        rgbTab.add(rgbCenter, BorderLayout.CENTER);

        // ---------- Собираем вкладки ----------
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabs.addTab("Цвета",   colorsTab);
        tabs.addTab("Пресеты", presetsTab);
        tabs.addTab("RGB",     rgbTab);

        // ---------- Кнопки внизу диалога ----------
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(bg);

        JButton okBtn     = new JButton("Готово");
        JButton cancelBtn = new JButton("Отмена");
        styleButton(okBtn, accent, Color.WHITE);
        styleButton(cancelBtn, field, text);

        okBtn.addActionListener(e -> {
            // Если работает RGB — не сбрасываем его
            if (!isRgbRunning()) {
                useCustom(chosen[0], chosen[1], chosen[2]);
                onApply.run();
            }
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> {
            stopRgb();
            dialog.dispose();
        });

        buttons.add(cancelBtn);
        buttons.add(okBtn);

        dialog.add(tabs, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /** Маленький цветной бокс с подписью (для вкладки "Пресеты"). */
    private static JPanel makePreview(String title) {
        JPanel box = new JPanel();
        box.setBackground(Theme.bg);
        box.setPreferredSize(new Dimension(80,60));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                new EmptyBorder(2,2,2,2)));
        return box;
    }

    /**
     * Строка "Название — цветной прямоугольник".
     * Клик по прямоугольнику открывает JColorChooser.
     */
    private static JPanel makeColorRow(String title, Color[] chosen, int index,
                                       Component parent, Runnable onApply) {
        JPanel row = new JPanel(new BorderLayout(8,0));
        row.setBackground(bg);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(text);

        JPanel preview = new JPanel();
        preview.setBackground(chosen[index]);
        preview.setBorder(BorderFactory.createLineBorder(border));
        preview.setPreferredSize(new Dimension(80,30));
        preview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        preview.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Color c = JColorChooser.showDialog(parent, "Выберите цвет", chosen[index]);
                if (c != null) {
                    chosen[index] = c;
                    preview.setBackground(c);
                    // Мгновенно применяем
                    useCustom(chosen[0], chosen[1], chosen[2]);
                    onApply.run();
                }
            }
        });

        row.add(lbl, BorderLayout.WEST);
        row.add(preview, BorderLayout.EAST);
        return row;
    }
}