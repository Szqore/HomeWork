package studio; //само окно и роль Приёмная

// Подключение стандартных пакетов Java для создания графического интерфейса
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
public class PCRepairStudio extends JFrame {

    // Список (коллекция) для хранения всех текущих заявок в оперативной памяти
    private final List<order> orders = new ArrayList<>();

    // Карта (словарь) базовых услуг студии: Название услуги -> Стоимость в рублях
    private final Map<String, Integer> services = new LinkedHashMap<>();

    // --- Компоненты текстовой формы создания новой заявки ---
    private JTextField fName;      // Текстовое поле для ввода ФИО клиента
    private JTextField fPhone;     // Текстовое поле для ввода номера телефона
    private JTextField fModel;     // Текстовое поле для ввода общей модели ПК
    private JComboBox<String> fIssue;   // Выпадающий список типов неисправностей
    private JComboBox<String> fUrgency; // Выпадающий список категорий срочности заказа
    private JTextArea fDesc;       // Большое многострочное поле описания проблемы

    // --- Компоненты интерактивного интерфейса блока Апгрейда ---
    private JPanel upgradePanel;        // Главный контейнер (панель) для блока апгрейда
    private JComboBox<String> myBoardBox; // Выпадающий список с материнской платой клиента
    private JComboBox<String> workBox;    // Выпадающий список с типом требуемой работы
    private JComboBox<String> cpuBox;     // Выпадающий список для выбора нового процессора
    private JComboBox<String> ramBox;     // Выпадающий список для выбора новой памяти (ОЗУ)
    private JComboBox<String> gpuBox;     // Выпадающий список для выбора новой видеокарты
    private JCheckBox keepOwnCheck;     // Флажок: "Уже есть свои комплектующие"
    private JLabel compatLabel;         // Текстовая метка статуса совместимости железа
    private JLabel upgradeTotalLabel;    // Текстовая метка итоговой стоимости апгрейда

    // Вспомогательные строковые панели-контейнеры для управления отображением строк апгрейда
    private JPanel boardRow, workRow, cpuRow, ramRow, gpuRow;

    // --- Компоненты вкладки полного списка заявок ---
    private DefaultTableModel tableModel; // Модель данных для управления строками таблицы
    private JTable table;                 // Графический элемент таблицы для вывода заявок
    private JTextField searchField;       // Текстовое поле для живого поиска по таблице

    // --- Компоненты экрана Калькулятора услуг ---
    private JComboBox<String> svcBox;     // Выпадающий список выбора рассчитываемой услуги
    private JSpinner qtySpin;             // Счётчик (спиннер) для изменения количества услуг
    private JSpinner discSpin;            // Счётчик (спиннер) для установки скидки в процентах
    private JLabel resultLabel;           // Крупное поле вывода итоговой стоимости с расчетом

    // --- Компоненты аналитики и стилизации ---
    private JTextArea statsArea;          // Панель для вывода текстового отчета статистики
    private boolean dark = false;         // Флаг текущего состояния темы (false - светлая)

    // Базовые панели разметки главного окна приложения
    private JPanel sideBar;    // Левое навигационное меню кнопок
    private JPanel headerBar;  // Верхняя шапка программы с названием и темой
    private JPanel contentArea;// Центральная область для переключения экранов (CardLayout)
    private JButton themeBtn;  // Кнопка быстрого переключения цветовой темы оформления

    // Списки для динамического изменения цветов всех компонентов при смене темы
    private final List<JComponent> allPanels = new ArrayList<>();
    private final List<JLabel> allLabels = new ArrayList<>();

    public PCRepairStudio() {
        setTitle("Студия ремонта ПК");
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        initServices();

        // Синхронизация: Загружаем свежие данные из базы MS Access
        orders.addAll(JsonDataManager.loadData());

        headerBar = new JPanel(new BorderLayout());
        headerBar.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel appTitle = new JLabel("Студия ремонта ПК");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerBar.add(appTitle, BorderLayout.WEST);

        themeBtn = new JButton("Тёмная тема");
        themeBtn.setFocusPainted(false);
        themeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        themeBtn.addActionListener(e -> toggleTheme());
        headerBar.add(themeBtn, BorderLayout.EAST);

        add(headerBar, BorderLayout.NORTH);

        sideBar = new JPanel();
        sideBar.setLayout(new BoxLayout(sideBar, BoxLayout.Y_AXIS));
        sideBar.setBorder(new EmptyBorder(20, 10, 20, 10));
        sideBar.setPreferredSize(new Dimension(210, 0));

        contentArea = new JPanel(new CardLayout());

        addSideButton("Новая заявка", "new");
        addSideButton("Список заявок", "list");
        addSideButton("Калькулятор", "calc");
        addSideButton("Статистика", "stats");

        contentArea.add(buildNewOrderPanel(), "new");
        contentArea.add(buildOrdersPanel(), "list");
        contentArea.add(buildCalcPanel(), "calc");
        contentArea.add(buildStatsPanel(), "stats");

        add(sideBar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        am.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JsonDataManager.saveData(orders, true);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTable();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JsonDataManager.saveData(orders, false);
                System.exit(0);
            }
        });

        refreshTable();
        updateStats();
        applyTheme();
    }

    private void addLabel(JPanel p, String text, int row, GridBagConstraints c) {
        JLabel lbl = new JLabel(text + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lbl);
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(lbl, c);
    }

    private void addField(JPanel p, JComponent field, int row, GridBagConstraints c) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.gridx = 1;
        c.gridy = row;
        c.weightx = 1;
        c.gridwidth = 1;
        p.add(field, c);
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        return b;
    }

    private void addSideButton(String text, String card) {
        JButton btn = new JButton(text);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> ((CardLayout) contentArea.getLayout()).show(contentArea, card));
        sideBar.add(btn);
        sideBar.add(Box.createVerticalStrut(4));
    }

    private void initServices() {
        services.put("Диагностика", 500);
        services.put("Чистка от пыли", 800);
        services.put("Замена термопасты", 700);
        services.put("Установка Windows", 1000);
        services.put("Удаление вирусов", 1200);
        services.put("Ремонт материнской платы", 3500);
        services.put("Восстановление данных", 2500);
    }

    private JPanel buildNewOrderPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 30, 20, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Оформление новой заявки");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        allPanels.add(form);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;

        addLabel(form, "ФИО клиента", row, c);
        fName = new JTextField();
        addField(form, fName, row++, c);

        addLabel(form, "Телефон", row, c);
        fPhone = new JTextField();
        addField(form, fPhone, row++, c);

        addLabel(form, "Модель ПК (общая)", row, c);
        fModel = new JTextField();
        addField(form, fModel, row++, c);

        addLabel(form, "Тип неисправности", row, c);
        fIssue = new JComboBox<>(new String[]{
                "Не включается", "Перегрев", "Вирус", "Замена комплектующих",
                "Чистка от пыли", "Установка ПО", "Диагностика", "Другое"});
        fIssue.addActionListener(e -> toggleUpgradeBlock());
        addField(form, fIssue, row++, c);

        addLabel(form, "Срочность", row, c);
        fUrgency = new JComboBox<>(new String[]{
                "Обычная", "Срочная (x1.5)", "Экспресс (x2)"});
        addField(form, fUrgency, row++, c);

        addLabel(form, "Описание проблемы", row, c);
        fDesc = new JTextArea(3, 30);
        fDesc.setLineWrap(true);
        fDesc.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(fDesc);
        c.gridx = 1;
        c.gridy = row++;
        c.weightx = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        form.add(descScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;

        upgradePanel = buildUpgradeBlock();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 8, 6, 8);
        form.add(upgradePanel, c);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(formScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        allPanels.add(buttons);

        JButton btnAdd = styledButton("Добавить заявку", new Color(46, 125, 50));
        btnAdd.addActionListener(e -> addOrder());

        JButton btnClear = styledButton("Очистить", new Color(120, 120, 120));
        btnClear.addActionListener(e -> clearForm());

        buttons.add(btnClear);
        buttons.add(btnAdd);
        root.add(buttons, BorderLayout.SOUTH);

        toggleUpgradeBlock();
        return root;
    }

    private JPanel buildUpgradeBlock() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Апгрейд / замена комплектующих"),
                new EmptyBorder(10, 10, 10, 10)));
        allPanels.add(p);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        boardRow = new JPanel(new BorderLayout(8, 0));
        boardRow.setOpaque(false);
        JLabel lblBoard = new JLabel("Ваша материнская плата:");
        lblBoard.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lblBoard);
        myBoardBox = new JComboBox<>(Hardware.MOTHERBOARDS.keySet().toArray(new String[0]));
        myBoardBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        myBoardBox.addActionListener(e -> checkCompatibility());
        boardRow.add(lblBoard, BorderLayout.WEST);
        boardRow.add(myBoardBox, BorderLayout.CENTER);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        p.add(boardRow, c);

        workRow = new JPanel(new BorderLayout(8, 0));
        workRow.setOpaque(false);
        JLabel lblWork = new JLabel("Что нужно сделать:");
        lblWork.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lblWork);
        workBox = new JComboBox<>(Hardware.WORK_TYPES);
        workBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        workBox.addActionListener(e -> {
            toggleUpgradeFields();
            checkCompatibility();
        });
        workRow.add(lblWork, BorderLayout.WEST);
        workRow.add(workBox, BorderLayout.CENTER);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        p.add(workRow, c);

        keepOwnCheck = new JCheckBox("Уже есть свои комплектующие (только замена)");
        keepOwnCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        keepOwnCheck.setOpaque(false);
        keepOwnCheck.addActionListener(e -> {
            toggleUpgradeFields();
            checkCompatibility();
        });
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        p.add(keepOwnCheck, c);

        cpuRow = new JPanel(new BorderLayout(8, 0));
        cpuRow.setOpaque(false);
        JLabel lblCpu = new JLabel("Новый процессор:");
        lblCpu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lblCpu);
        cpuBox = new JComboBox<>(Hardware.CPUS.keySet().toArray(new String[0]));
        cpuBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cpuBox.addActionListener(e -> {
            checkCompatibility();
            updateUpgradeTotal();
        });
        cpuRow.add(lblCpu, BorderLayout.WEST);
        cpuRow.add(cpuBox, BorderLayout.CENTER);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        p.add(cpuRow, c);

        ramRow = new JPanel(new BorderLayout(8, 0));
        ramRow.setOpaque(false);
        JLabel lblRam = new JLabel("Новая оперативная память:");
        lblRam.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lblRam);
        ramBox = new JComboBox<>(Hardware.RAM.keySet().toArray(new String[0]));
        ramBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ramBox.addActionListener(e -> {
            checkCompatibility();
            updateUpgradeTotal();
        });
        ramRow.add(lblRam, BorderLayout.WEST);
        ramRow.add(ramBox, BorderLayout.CENTER);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        p.add(ramRow, c);

        gpuRow = new JPanel(new BorderLayout(8, 0));
        gpuRow.setOpaque(false);
        JLabel lblGpu = new JLabel("Новая видеокарта:");
        lblGpu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lblGpu);
        gpuBox = new JComboBox<>(Hardware.GPUS.keySet().toArray(new String[0]));
        gpuBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gpuBox.addActionListener(e -> updateUpgradeTotal());
        gpuRow.add(lblGpu, BorderLayout.WEST);
        gpuRow.add(gpuBox, BorderLayout.CENTER);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        p.add(gpuRow, c);

        compatLabel = new JLabel(" ");
        compatLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        allLabels.add(compatLabel);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        p.add(compatLabel, c);

        upgradeTotalLabel = new JLabel("Стоимость апгрейда: 0 руб.");
        upgradeTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        allLabels.add(upgradeTotalLabel);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        p.add(upgradeTotalLabel, c);

        return p;
    }

    private void toggleUpgradeBlock() {
        if (upgradePanel == null || fIssue == null) return;
        String issue = (String) fIssue.getSelectedItem();
        boolean show = "Замена комплектующих".equals(issue);
        upgradePanel.setVisible(show);

        if (upgradePanel.getParent() != null) {
            upgradePanel.getParent().revalidate();
            upgradePanel.getParent().repaint();
        }
        if (show) checkCompatibility();
    }

    private void toggleUpgradeFields() {
        if (workBox == null || keepOwnCheck == null || cpuRow == null || ramRow == null || gpuRow == null) return;

        String work = (String) workBox.getSelectedItem();
        boolean keepOwn = keepOwnCheck.isSelected();

        boolean showCpu = "Заменить процессор".equals(work) && !keepOwn;
        boolean showRam = "Заменить оперативную память".equals(work) && !keepOwn;
        boolean showGpu = "Заменить видеокарту".equals(work) && !keepOwn;

        cpuRow.setVisible(showCpu);
        ramRow.setVisible(showRam);
        gpuRow.setVisible(showGpu);

        if (upgradePanel != null && upgradePanel.getParent() != null) {
            upgradePanel.revalidate();
            upgradePanel.repaint();
        }
        updateUpgradeTotal();
    }

    private void checkCompatibility() {
        if (myBoardBox == null || compatLabel == null) return;

        String board = (String) myBoardBox.getSelectedItem();
        if (board == null || !Hardware.MOTHERBOARDS.containsKey(board)) {
            compatLabel.setText(" ");
            return;
        }

        String[] specs = Hardware.MOTHERBOARDS.get(board);
        String boardSocket = specs[0];
        String boardRam = specs[1];

        List<String> errors = new ArrayList<>();
        List<String> ok = new ArrayList<>();

        if (cpuRow != null && cpuRow.isVisible()) {
            String cpu = (String) cpuBox.getSelectedItem();
            String cpuSocket = Hardware.CPUS.get(cpu);
            if (cpuSocket != null) {
                if (cpuSocket.equals(boardSocket)) {
                    ok.add("CPU: " + cpu + " (" + cpuSocket + ")");
                } else {
                    errors.add("CPU " + cpu + " имеет сокет " + cpuSocket + ", а плата поддерживает сокет " + boardSocket);
                }
            }
        }

        if (ramRow != null && ramRow.isVisible()) {
            String ram = (String) ramBox.getSelectedItem();
            String ramType = Hardware.RAM.get(ram);
            if (ramType != null) {
                if (ramType.equals(boardRam)) {
                    ok.add("RAM: " + ram + " (" + ramType + ")");
                } else {
                    errors.add("RAM " + ram + " имеет тип " + ramType + ", а плата требует тип " + boardRam);
                }
            }
        }

        if (!errors.isEmpty()) {
            compatLabel.setText("Несовместимо: " + String.join("; ", errors));
            compatLabel.setForeground(new Color(200, 30, 30));
        } else if (!ok.isEmpty()) {
            compatLabel.setText("Совместимо. " + String.join("; ", ok));
            compatLabel.setForeground(new Color(30, 150, 30));
        } else {
            compatLabel.setText(" ");
        }
        updateUpgradeTotal();
    }

    private void updateUpgradeTotal() {
        if (upgradePanel == null || !upgradePanel.isVisible()) return;
        if (workBox == null || keepOwnCheck == null || upgradeTotalLabel == null) return;

        int total = 0;
        String work = (String) workBox.getSelectedItem();
        boolean keepOwn = keepOwnCheck.isSelected();

        total += Hardware.WORK_PRICES.getOrDefault(work, 0);

        if (!keepOwn) {
            if (cpuRow != null && cpuRow.isVisible())
                total += Hardware.PRICES.getOrDefault((String) cpuBox.getSelectedItem(), 0);
            if (ramRow != null && ramRow.isVisible())
                total += Hardware.PRICES.getOrDefault((String) ramBox.getSelectedItem(), 0);
            if (gpuRow != null && gpuRow.isVisible())
                total += Hardware.PRICES.getOrDefault((String) gpuBox.getSelectedItem(), 0);
        }
        upgradeTotalLabel.setText("Стоимость апгрейда: " + total + " руб.");
    }

    private String buildUpgradeDescription() {
        if (upgradePanel == null || !upgradePanel.isVisible()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Плата: ").append(myBoardBox.getSelectedItem()).append("\n");
        sb.append("Работа: ").append(workBox.getSelectedItem()).append("\n");
        if (keepOwnCheck.isSelected()) sb.append("Комплектующие: свои\n");
        if (cpuRow.isVisible()) sb.append("CPU: ").append(cpuBox.getSelectedItem()).append("\n");
        if (ramRow.isVisible()) sb.append("RAM: ").append(ramBox.getSelectedItem()).append("\n");
        if (gpuRow.isVisible()) sb.append("GPU: ").append(gpuBox.getSelectedItem()).append("\n");
        sb.append("Совместимость: ").append(compatLabel.getText()).append("\n");
        sb.append("Цена апгрейда: ").append(upgradeTotalLabel.getText().replace("Стоимость апгрейда: ", ""));
        return sb.toString();
    }

    private JPanel buildOrdersPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Список заявок");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        root.add(title, BorderLayout.NORTH);

        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        allPanels.add(searchBar);
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshTable();
            }
        });

        JButton btnReset = styledButton("Сброс", new Color(120, 120, 120));
        btnReset.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(btnReset, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        allPanels.add(center);
        center.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"№", "Клиент", "Телефон", "Модель", "Неисправность", "Срочность", "Дата"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 4));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showDetails();
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        allPanels.add(buttons);

        JButton btnDetails = styledButton("Детали", new Color(33, 150, 243));
        btnDetails.addActionListener(e -> showDetails());

        JButton btnDelete = styledButton("Удалить", new Color(198, 40, 40));
        btnDelete.addActionListener(e -> deleteOrder());

        JButton btnClearAll = styledButton("Очистить всё", new Color(120, 120, 120));
        btnClearAll.addActionListener(e -> clearAll());

        JButton btnSave = styledButton("Сохранить", new Color(46, 125, 50));
        btnSave.addActionListener(e -> JsonDataManager.saveData(orders, true));

        buttons.add(btnDetails);
        buttons.add(btnDelete);
        buttons.add(btnClearAll);
        buttons.add(btnSave);

        root.add(buttons, BorderLayout.SOUTH);

        return root;
    }

    private JPanel buildCalcPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Калькулятор стоимости работ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        root.add(title, c);
        c.gridwidth = 1;

        addLabel(root, "Услуга", 1, c);
        svcBox = new JComboBox<>(services.keySet().toArray(new String[0]));
        svcBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        svcBox.addActionListener(e -> calcPrice());
        addField(root, svcBox, 1, c);

        addLabel(root, "Количество", 2, c);
        qtySpin = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        qtySpin.addChangeListener(e -> calcPrice());
        addField(root, qtySpin, 2, c);

        addLabel(root, "Скидка, %", 3, c);
        discSpin = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
        discSpin.addChangeListener(e -> calcPrice());
        addField(root, discSpin, 3, c);

        resultLabel = new JLabel("Итого: 0 руб.");
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        resultLabel.setOpaque(true);
        allLabels.add(resultLabel);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(25, 8, 8, 8);
        root.add(resultLabel, c);

        calcPrice();
        return root;
    }

    private JPanel buildStatsPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Статистика студии");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        root.add(title, BorderLayout.NORTH);

        statsArea = new JTextArea();
        statsArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        statsArea.setEditable(false);
        statsArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.add(new JScrollPane(statsArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        allPanels.add(bottom);
        JButton btnUpdate = styledButton("Обновить", new Color(33, 150, 243));
        btnUpdate.addActionListener(e -> updateStats());
        bottom.add(btnUpdate);
        root.add(bottom, BorderLayout.SOUTH);

        return root;
    }

    private void addOrder() {
        String name = fName.getText().trim();
        String phone = fPhone.getText().trim();
        String model = fModel.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните ФИО, телефон и модель ПК!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (upgradePanel.isVisible() && compatLabel.getText().startsWith("Несовместимо")) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Обнаружена несовместимость комплектующих:\n\n" + compatLabel.getText() + "\n\nВсё равно добавить заявку?",
                    "Несовместимость", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) return;
        }

        int newId = orders.stream().mapToInt(o -> o.id).max().orElse(0) + 1;
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String desc = fDesc.getText().trim();
        String upgrade = buildUpgradeDescription();
        if (!upgrade.isEmpty()) {
            if (!desc.isEmpty()) desc += "\n\n--- Апгрейд ---\n";
            else desc = "--- Апгрейд ---\n";
            desc += upgrade;
        }

        orders.add(new order(newId, name, phone, model, (String) fIssue.getSelectedItem(), (String) fUrgency.getSelectedItem(), desc, date));

        // Вызов сохранения в Access вместо старого JSON-файла
        JsonDataManager.saveData(orders, false);

        refreshTable();
        updateStats();
        clearForm();

        JOptionPane.showMessageDialog(this, "Заявка №" + newId + " успешно добавлена!", "Успех", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm() {
        fName.setText("");
        fPhone.setText("");
        fModel.setText("");
        fDesc.setText("");
        fIssue.setSelectedIndex(0);
        fUrgency.setSelectedIndex(0);
        myBoardBox.setSelectedIndex(0);
        workBox.setSelectedIndex(0);
        keepOwnCheck.setSelected(false);
        cpuBox.setSelectedIndex(0);
        ramBox.setSelectedIndex(0);
        gpuBox.setSelectedIndex(0);
        toggleUpgradeBlock();
    }

    private void deleteOrder() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите заявку в таблице!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Удалить заявку №" + id + "?", "Подтверждение", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        // Удаление строки непосредственно из базы данных Access
        JsonDataManager.deleteOrderFromDb(id);

        // Синхронизируем коллекцию в оперативной памяти с базой данных
        orders.clear();
        orders.addAll(JsonDataManager.loadData());

        refreshTable();
        updateStats();
    }

    private void clearAll() {
        if (orders.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Удалить ВСЕ заявки из базы безвозвратно?", "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (order o : orders) {
                JsonDataManager.deleteOrderFromDb(o.id);
            }
            orders.clear();
            orders.addAll(JsonDataManager.loadData());
            refreshTable();
            updateStats();
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        for (order o : orders) {
            String all = (o.id + " " + o.name + " " + o.phone + " " + o.model + " " + o.issue + " " + o.urgency + " " + o.date).toLowerCase();
            if (!q.isEmpty() && !all.contains(q)) continue;
            tableModel.addRow(new Object[]{o.id, o.name, o.phone, o.model, o.issue, o.urgency, o.date});
        }
    }

    private void showDetails() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (Integer) tableModel.getValueAt(row, 0);
        order o = orders.stream().filter(x -> x.id == id).findFirst().orElse(null);
        if (o == null) return;

        JTextArea area = new JTextArea(20, 55);
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setText(
                "Заявка №" + o.id + "\n" + "Дата создания: " + o.date + "\n\n" +
                        "Клиент: " + o.name + "\n" + "Телефон для связи: " + o.phone + "\n" +
                        "Модель устройства: " + o.model + "\n" + "Категория поломки: " + o.issue + "\n" +
                        "Срочность ремонта: " + o.urgency + "\n\n" +
                        "Техническое описание проблемы:\n" + (o.desc.isEmpty() ? "—" : o.desc));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, sp, "Детальная карточка заявки", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calcPrice() {
        String svc = (String) svcBox.getSelectedItem();
        int qty = (Integer) qtySpin.getValue();
        int disc = (Integer) discSpin.getValue();
        int base = services.getOrDefault(svc, 0);
        double total = base * qty * (1 - disc / 100.0);
        resultLabel.setText(String.format("%s × %d = %.2f руб.  (скидка %d%%)", svc, qty, total, disc));
    }

    private void updateStats() {
        int total = orders.size();
        long urgent = orders.stream().filter(o -> o.urgency.contains("Срочная")).count();
        long express = orders.stream().filter(o -> o.urgency.contains("Экспресс")).count();

        Map<String, Integer> count = new HashMap<>();
        for (order o : orders) count.put(o.issue, count.getOrDefault(o.issue, 0) + 1);

        StringBuilder sb = new StringBuilder();
        sb.append("Всего заявок в базе: ").append(total).append("\n");
        sb.append("Срочного типа:       ").append(urgent).append("\n");
        sb.append("Экспресс заказов:    ").append(express).append("\n\n");
        sb.append("Топ частых неисправностей:\n");

        count.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .forEach(e -> sb.append("   • ").append(e.getKey()).append(": ").append(e.getValue()).append(" шт.\n"));

        if (count.isEmpty()) sb.append("   — нет данных для анализа —");
        statsArea.setText(sb.toString());
    }

    private void toggleTheme() {
        dark = !dark;
        themeBtn.setText(dark ? "Светлая тема" : "Тёмная тема");
        applyTheme();
    }

    private void applyTheme() {
        Color bg, panelBg, textColor, subtle, accent;
        if (dark) {
            bg = new Color(24, 26, 32);
            panelBg = new Color(34, 37, 45);
            textColor = new Color(120, 210, 255);
            subtle = new Color(60, 65, 80);
            accent = new Color(50, 140, 200);
        } else {
            bg = new Color(250, 250, 252);
            panelBg = new Color(240, 242, 246);
            textColor = new Color(30, 30, 30);
            subtle = new Color(210, 215, 220);
            accent = new Color(33, 150, 243);
        }

        getContentPane().setBackground(bg);
        headerBar.setBackground(panelBg);
        headerBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, subtle));
        for (Component c : headerBar.getComponents()) {
            c.setBackground(panelBg);
            c.setForeground(textColor);
        }

        sideBar.setBackground(panelBg);
        sideBar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, subtle));
        for (Component c : sideBar.getComponents()) {
            c.setBackground(panelBg);
            c.setForeground(textColor);
        }

        contentArea.setBackground(bg);
        for (JComponent p : allPanels) {
            p.setBackground(bg);
            p.setForeground(textColor);
            if (p instanceof JPanel) p.setOpaque(true);
        }
        for (JLabel l : allLabels) l.setForeground(textColor);

        for (Component c : getAllDescendants(contentArea)) {
            if (c instanceof JTextField) {
                c.setBackground(dark ? new Color(45, 48, 58) : Color.WHITE);
                c.setForeground(textColor);
                ((JTextField) c).setCaretColor(textColor);
                ((JTextField) c).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(subtle, 1), new EmptyBorder(6, 8, 6, 8)));
            } else if (c instanceof JTextArea) {
                c.setBackground(dark ? new Color(45, 48, 58) : Color.WHITE);
                c.setForeground(textColor);
                ((JTextArea) c).setCaretColor(textColor);
            } else if (c instanceof JComboBox || c instanceof JSpinner) {
                c.setBackground(dark ? new Color(45, 48, 58) : Color.WHITE);
                c.setForeground(textColor);
            } else if (c instanceof JCheckBox) {
                c.setForeground(textColor);
            } else if (c instanceof JScrollPane) {
                c.setBackground(bg);
                ((JScrollPane) c).getViewport().setBackground(bg);
                ((JScrollPane) c).setBorder(BorderFactory.createLineBorder(subtle, 1));
            }
        }

        table.setBackground(dark ? new Color(34, 37, 45) : Color.WHITE);
        table.setForeground(textColor);
        table.setSelectionBackground(accent);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(panelBg);
        table.getTableHeader().setForeground(textColor);
        statsArea.setBackground(dark ? new Color(34, 37, 45) : Color.WHITE);
        statsArea.setForeground(textColor);
        if (resultLabel != null) {
            resultLabel.setBackground(dark ? new Color(30, 60, 90) : new Color(225, 240, 255));
            resultLabel.setForeground(dark ? new Color(150, 220, 255) : new Color(10, 60, 120));
            resultLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 1), new EmptyBorder(20, 20, 20, 20)));
        }

        themeBtn.setBackground(dark ? new Color(50, 140, 200) : new Color(240, 240, 240));
        themeBtn.setForeground(dark ? Color.WHITE : Color.BLACK);
        themeBtn.setBorder(new EmptyBorder(6, 14, 6, 14)); themeBtn.setOpaque(true);

        if (upgradePanel != null && upgradePanel.isVisible()) {
            checkCompatibility();
        }
        repaint(); // Полная принудительная перерисовка всего окна операционной системой
    }

    /**
     * Вспомогательный алгоритм для рекурсивного сбора абсолютно всех вложенных компонентов панели.
     */
    private List<Component> getAllDescendants(Container root) {
        List<Component> list = new ArrayList<>();
        for (Component c : root.getComponents()) {
            list.add(c);
            if (c instanceof Container) {
                list.addAll(getAllDescendants((Container) c));
            }
        }
        return list;
    }
}
