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

    private final User currentUser;
    private final List<order> orders = new ArrayList<>();
    private final Map<String, Integer> services = new LinkedHashMap<>();

    private JTextField fName, fPhone, fModel;
    private JComboBox<String> fIssue, fUrgency;
    private JTextArea fDesc;

    private JPanel upgradePanel;
    private JComboBox<String> myBoardBox, workBox, cpuBox, ramBox, gpuBox;
    private JCheckBox keepOwnCheck;
    private JLabel compatLabel, upgradeTotalLabel;
    private JPanel boardRow, workRow, cpuRow, ramRow, gpuRow;

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;

    private DefaultTableModel myTableModel;
    private JTable myTable;

    private DefaultTableModel archiveModel;
    private JTable archiveTable;

    private JComboBox<String> svcBox;
    private JSpinner qtySpin, discSpin;
    private JLabel resultLabel;

    private JTextArea statsArea;
    private boolean dark = false;

    private JPanel sideBar, headerBar, contentArea;
    private JButton themeBtn;

    private final List<JComponent> allPanels = new ArrayList<>();
    private final List<JLabel> allLabels = new ArrayList<>();

    private boolean isOwner() { return currentUser.isOwner(); }
    private boolean isJob()   { return currentUser.isJob(); }
    private boolean isUser()  { return !isOwner() && !isJob(); }

    public PCRepairStudio(User user) {
        this.currentUser = user;

        setTitle("Студия ремонта ПК — " + currentUser.fullName + " [" + currentUser.role + "]");
        setSize(1180, 720);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        initServices();
        orders.addAll(JsonDataManager.loadData());

        headerBar = new JPanel(new BorderLayout());
        headerBar.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel appTitle = new JLabel("Студия ремонта ПК  |  " + currentUser.fullName +
                "  [" + currentUser.role + "]");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerBar.add(appTitle, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        themeBtn = new JButton("Тёмная тема");
        themeBtn.setFocusPainted(false);
        themeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        themeBtn.addActionListener(e -> toggleTheme());
        headerRight.add(themeBtn);

        JButton logoutBtn = new JButton("Выйти");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutBtn.addActionListener(e -> doLogout());
        headerRight.add(logoutBtn);

        headerBar.add(headerRight, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        sideBar = new JPanel();
        sideBar.setLayout(new BoxLayout(sideBar, BoxLayout.Y_AXIS));
        sideBar.setBorder(new EmptyBorder(20, 10, 20, 10));
        sideBar.setPreferredSize(new Dimension(210, 0));

        contentArea = new JPanel(new CardLayout());

        if (isUser()) {
            addSideButton("Новая заявка", "new");
            addSideButton("Мои заявки", "mine");
            addSideButton("Калькулятор", "calc");

            contentArea.add(buildNewOrderPanel(), "new");
            contentArea.add(buildMyOrdersPanel(), "mine");
            contentArea.add(buildCalcPanel(), "calc");

        } else if (isJob()) {
            addSideButton("Список заявок", "list");
            addSideButton("Калькулятор", "calc");

            contentArea.add(buildOrdersPanel(), "list");
            contentArea.add(buildCalcPanel(), "calc");

        } else {
            addSideButton("Новая заявка", "new");
            addSideButton("Список заявок", "list");
            addSideButton("Архив", "archive");
            addSideButton("Калькулятор", "calc");
            addSideButton("Прибыль", "stats");
            addSideButton("Аккаунты", "accounts");

            contentArea.add(buildNewOrderPanel(), "new");
            contentArea.add(buildOrdersPanel(), "list");
            contentArea.add(buildArchivePanel(), "archive");
            contentArea.add(buildCalcPanel(), "calc");
            contentArea.add(buildStatsPanel(), "stats");
            contentArea.add(buildAccountsPanel(), "accounts");
        }

        add(sideBar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);

        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        am.put("save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                JsonDataManager.saveData(orders, true);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put("refresh", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                refreshTable();
                refreshMyTable();
                refreshArchive();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        refreshTable();
        refreshMyTable();
        if (statsArea != null) updateStats();
        applyTheme();
    }

    private void doLogout() {
        if (JOptionPane.showConfirmDialog(this,
                "Выйти из учётной записи?", "Выход",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        dispose();

        SwingUtilities.invokeLater(() -> {
            LoginWindow lw = new LoginWindow();
            lw.setVisible(true);
            lw.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e2) {
                    User u = lw.getAuthenticatedUser();
                    if (u != null) new PCRepairStudio(u).setVisible(true);
                    else System.exit(0);
                }
            });
        });
    }

    private void addLabel(JPanel p, String text, int row, GridBagConstraints c) {
        JLabel lbl = new JLabel(text + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        allLabels.add(lbl);
        c.gridx = 0; c.gridy = row; c.weightx = 0; c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(lbl, c);
    }

    private void addField(JPanel p, JComponent field, int row, GridBagConstraints c) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.gridx = 1; c.gridy = row; c.weightx = 1; c.gridwidth = 1;
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
        fName = new JTextField(); addField(form, fName, row++, c);

        addLabel(form, "Телефон", row, c);
        fPhone = new JTextField(); addField(form, fPhone, row++, c);

        addLabel(form, "Модель ПК (общая)", row, c);
        fModel = new JTextField(); addField(form, fModel, row++, c);

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
        fDesc.setLineWrap(true); fDesc.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(fDesc);
        c.gridx = 1; c.gridy = row++; c.weightx = 1; c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        form.add(descScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;

        upgradePanel = buildUpgradeBlock();
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
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
        lblBoard.setFont(new Font("Segoe UI", Font.PLAIN, 13)); allLabels.add(lblBoard);
        myBoardBox = new JComboBox<>(Hardware.MOTHERBOARDS.keySet().toArray(new String[0]));
        myBoardBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        myBoardBox.addActionListener(e -> checkCompatibility());
        boardRow.add(lblBoard, BorderLayout.WEST);
        boardRow.add(myBoardBox, BorderLayout.CENTER);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(boardRow, c);

        workRow = new JPanel(new BorderLayout(8, 0));
        workRow.setOpaque(false);
        JLabel lblWork = new JLabel("Что нужно сделать:");
        lblWork.setFont(new Font("Segoe UI", Font.PLAIN, 13)); allLabels.add(lblWork);
        workBox = new JComboBox<>(Hardware.WORK_TYPES);
        workBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        workBox.addActionListener(e -> { toggleUpgradeFields(); checkCompatibility(); });
        workRow.add(lblWork, BorderLayout.WEST);
        workRow.add(workBox, BorderLayout.CENTER);
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; p.add(workRow, c);

        keepOwnCheck = new JCheckBox("Уже есть свои комплектующие (только замена)");
        keepOwnCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        keepOwnCheck.setOpaque(false);
        keepOwnCheck.addActionListener(e -> { toggleUpgradeFields(); checkCompatibility(); });
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; p.add(keepOwnCheck, c);

        cpuRow = new JPanel(new BorderLayout(8, 0));
        cpuRow.setOpaque(false);
        JLabel lblCpu = new JLabel("Новый процессор:");
        lblCpu.setFont(new Font("Segoe UI", Font.PLAIN, 13)); allLabels.add(lblCpu);
        cpuBox = new JComboBox<>(Hardware.CPUS.keySet().toArray(new String[0]));
        cpuBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cpuBox.addActionListener(e -> { checkCompatibility(); updateUpgradeTotal(); });
        cpuRow.add(lblCpu, BorderLayout.WEST);
        cpuRow.add(cpuBox, BorderLayout.CENTER);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; p.add(cpuRow, c);

        ramRow = new JPanel(new BorderLayout(8, 0));
        ramRow.setOpaque(false);
        JLabel lblRam = new JLabel("Новая оперативная память:");
        lblRam.setFont(new Font("Segoe UI", Font.PLAIN, 13)); allLabels.add(lblRam);
        ramBox = new JComboBox<>(Hardware.RAM.keySet().toArray(new String[0]));
        ramBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ramBox.addActionListener(e -> { checkCompatibility(); updateUpgradeTotal(); });
        ramRow.add(lblRam, BorderLayout.WEST);
        ramRow.add(ramBox, BorderLayout.CENTER);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; p.add(ramRow, c);

        gpuRow = new JPanel(new BorderLayout(8, 0));
        gpuRow.setOpaque(false);
        JLabel lblGpu = new JLabel("Новая видеокарта:");
        lblGpu.setFont(new Font("Segoe UI", Font.PLAIN, 13)); allLabels.add(lblGpu);
        gpuBox = new JComboBox<>(Hardware.GPUS.keySet().toArray(new String[0]));
        gpuBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gpuBox.addActionListener(e -> updateUpgradeTotal());
        gpuRow.add(lblGpu, BorderLayout.WEST);
        gpuRow.add(gpuBox, BorderLayout.CENTER);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; p.add(gpuRow, c);

        compatLabel = new JLabel(" ");
        compatLabel.setFont(new Font("Segoe UI", Font.BOLD, 13)); allLabels.add(compatLabel);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; p.add(compatLabel, c);

        upgradeTotalLabel = new JLabel("Стоимость апгрейда: 0 руб.");
        upgradeTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14)); allLabels.add(upgradeTotalLabel);
        c.gridx = 0; c.gridy = 7; c.gridwidth = 2; p.add(upgradeTotalLabel, c);

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
        cpuRow.setVisible("Заменить процессор".equals(work) && !keepOwn);
        ramRow.setVisible("Заменить оперативную память".equals(work) && !keepOwn);
        gpuRow.setVisible("Заменить видеокарту".equals(work) && !keepOwn);
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
                if (cpuSocket.equals(boardSocket)) ok.add("CPU: " + cpu);
                else errors.add("CPU " + cpu + " не подходит");
            }
        }
        if (ramRow != null && ramRow.isVisible()) {
            String ram = (String) ramBox.getSelectedItem();
            String ramType = Hardware.RAM.get(ram);
            if (ramType != null) {
                if (ramType.equals(boardRam)) ok.add("RAM: " + ram);
                else errors.add("RAM " + ram + " не подходит");
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

    // ===================== СПИСОК ВСЕХ ЗАЯВОК (job, owner) =====================
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
            @Override public void keyReleased(KeyEvent e) { refreshTable(); }
        });
        JButton btnReset = styledButton("Сброс", new Color(120, 120, 120));
        btnReset.addActionListener(e -> { searchField.setText(""); refreshTable(); });
        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(btnReset, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        allPanels.add(center);
        center.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"№", "Клиент", "Телефон", "Модель", "Неисправность", "Срочность", "Прогресс", "Дата"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 4));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showDetails();
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        allPanels.add(buttons);

        JButton btnDetails = styledButton("Детали / Прогресс", new Color(33, 150, 243));
        btnDetails.addActionListener(e -> showDetails());
        buttons.add(btnDetails);

        if (isOwner() || isJob()) {
            JButton btnDelete = styledButton("Удалить", new Color(198, 40, 40));
            btnDelete.addActionListener(e -> deleteOrder());
            buttons.add(btnDelete);
        }

        if (isOwner()) {
            JButton btnClearAll = styledButton("Очистить всё", new Color(120, 120, 120));
            btnClearAll.addActionListener(e -> clearAll());
            buttons.add(btnClearAll);
        }

        JButton btnSave = styledButton("Сохранить", new Color(46, 125, 50));
        btnSave.addActionListener(e -> JsonDataManager.saveData(orders, true));
        buttons.add(btnSave);

        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    // ===================== МОИ ЗАЯВКИ (user) =====================
    private JPanel buildMyOrdersPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Мои заявки");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"№", "Модель", "Неисправность", "Прогресс", "Дата", "Оценка", "Комментарий"};
        myTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        myTable = new JTable(myTableModel);
        myTable.setRowHeight(28);
        myTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        myTable.setShowGrid(false);
        myTable.setIntercellSpacing(new Dimension(0, 4));
        myTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        myTable.getTableHeader().setReorderingAllowed(false);

        myTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showMyOrderDetails();
            }
        });

        root.add(new JScrollPane(myTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        allPanels.add(buttons);

        JButton btnDetails = styledButton("Подробнее", new Color(33, 150, 243));
        btnDetails.addActionListener(e -> showMyOrderDetails());
        buttons.add(btnDetails);

        JButton btnRate = styledButton("Оценить (готовую)", new Color(245, 160, 20));
        btnRate.addActionListener(e -> rateSelectedOrder());
        buttons.add(btnRate);

        JButton btnRefresh = styledButton("Обновить", new Color(120, 120, 120));
        btnRefresh.addActionListener(e -> refreshMyTable());
        buttons.add(btnRefresh);

        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    // ===================== АРХИВ (только owner) =====================
    private JPanel buildArchivePanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Архив выполненных заявок");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"№", "Клиент", "Телефон", "Модель", "Неисправность",
                "Срочность", "Дата закрытия", "Оценка", "Комментарий"};
        archiveModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        archiveTable = new JTable(archiveModel);
        archiveTable.setRowHeight(28);
        archiveTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        archiveTable.setShowGrid(false);
        archiveTable.setIntercellSpacing(new Dimension(0, 4));
        archiveTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        archiveTable.getTableHeader().setReorderingAllowed(false);

        archiveTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showArchiveDetails();
            }
        });

        root.add(new JScrollPane(archiveTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        allPanels.add(buttons);

        JButton btnDetails = styledButton("Подробнее", new Color(33, 150, 243));
        btnDetails.addActionListener(e -> showArchiveDetails());
        buttons.add(btnDetails);

        JButton btnRefresh = styledButton("Обновить", new Color(120, 120, 120));
        btnRefresh.addActionListener(e -> refreshArchive());
        buttons.add(btnRefresh);

        root.add(buttons, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::refreshArchive);
        return root;
    }

    // ===================== КАЛЬКУЛЯТОР =====================
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
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
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

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(25, 8, 8, 8);
        root.add(resultLabel, c);

        calcPrice();
        return root;
    }

    // ===================== ПРИБЫЛЬ / УБЫТОК =====================
    private JPanel buildStatsPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Прибыль / Убыток студии");
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

    // ===================== АККАУНТЫ =====================
    private JPanel buildAccountsPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        allPanels.add(root);

        JLabel title = new JLabel("Аккаунты системы");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        allLabels.add(title);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Логин", "Пароль (SHA-256)", "Роль", "ФИО"};
        DefaultTableModel accModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable accTable = new JTable(accModel);
        accTable.setRowHeight(28);
        accTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        accTable.setShowGrid(false);
        accTable.setIntercellSpacing(new Dimension(0, 4));
        accTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        accTable.getTableHeader().setReorderingAllowed(false);

        Runnable refreshAccounts = () -> {
            accModel.setRowCount(0);
            for (User u : AuthManager.getAllUsers()) {
                String pwd = u.password != null && u.password.length() > 16
                        ? u.password.substring(0, 16) + "..." : u.password;
                accModel.addRow(new Object[]{u.id, u.login, pwd, u.role, u.fullName});
            }
        };
        refreshAccounts.run();
        root.add(new JScrollPane(accTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        allPanels.add(bottom);

        JButton btnRefresh = styledButton("Обновить", new Color(33, 150, 243));
        btnRefresh.addActionListener(e -> refreshAccounts.run());
        bottom.add(btnRefresh);

        JButton btnAdd = styledButton("Добавить", new Color(46, 125, 50));
        btnAdd.addActionListener(e -> {
            JTextField l = new JTextField();
            JPasswordField p = new JPasswordField();
            JTextField f = new JTextField();
            JComboBox<String> r = new JComboBox<>(new String[]{"user", "job", "owner"});
            Object[] msg = {"Логин:", l, "Пароль:", p, "ФИО:", f, "Роль:", r};
            int res = JOptionPane.showConfirmDialog(this, msg, "Новый аккаунт",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            boolean ok = AuthManager.register(l.getText().trim(),
                    new String(p.getPassword()),
                    f.getText().trim().isEmpty() ? l.getText().trim() : f.getText().trim(),
                    (String) r.getSelectedItem());
            if (ok) { refreshAccounts.run(); JOptionPane.showMessageDialog(this, "Аккаунт добавлен!"); }
            else JOptionPane.showMessageDialog(this, "Ошибка: логин занят или пароль < 4 символов.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        });

        JButton btnDelete = styledButton("Удалить", new Color(198, 40, 40));
        btnDelete.addActionListener(e -> {
            int row = accTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Выберите аккаунт!"); return; }
            String login = (String) accModel.getValueAt(row, 1);
            if ("owner".equalsIgnoreCase(login)) {
                JOptionPane.showMessageDialog(this, "Владельца удалить нельзя!"); return;
            }
            if (JOptionPane.showConfirmDialog(this, "Удалить аккаунт " + login + "?",
                    "Подтверждение", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            if (AuthManager.deleteUser(login)) refreshAccounts.run();
        });

        bottom.add(btnDelete);
        bottom.add(btnAdd);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    // ===================== ДОБАВЛЕНИЕ ЗАЯВКИ =====================
    private void addOrder() {
        String name = fName.getText().trim();
        String phone = fPhone.getText().trim();
        String model = fModel.getText().trim();

        if (name.isEmpty() || phone.isEmpty() || model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните ФИО, телефон и модель ПК!",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (upgradePanel.isVisible() && compatLabel.getText().startsWith("Несовместимо")) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Несовместимость:\n\n" + compatLabel.getText() + "\n\nВсё равно добавить?",
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
        desc = "[Принял: " + currentUser.fullName + "]\n\n" + desc;

        order newOrder = new order(
                newId, name, phone, model,
                (String) fIssue.getSelectedItem(),
                (String) fUrgency.getSelectedItem(),
                desc, date, 0,
                currentUser.login, 0, ""
        );

        List<order> tmp = new ArrayList<>();
        tmp.add(newOrder);
        JsonDataManager.saveData(tmp, false);

        orders.clear();
        orders.addAll(JsonDataManager.loadData());

        refreshTable();
        refreshMyTable();
        if (statsArea != null) updateStats();
        clearForm();

        JOptionPane.showMessageDialog(this, "Заявка добавлена!",
                "Успех", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm() {
        fName.setText(""); fPhone.setText(""); fModel.setText(""); fDesc.setText("");
        fIssue.setSelectedIndex(0); fUrgency.setSelectedIndex(0);
        myBoardBox.setSelectedIndex(0); workBox.setSelectedIndex(0);
        keepOwnCheck.setSelected(false);
        cpuBox.setSelectedIndex(0); ramBox.setSelectedIndex(0); gpuBox.setSelectedIndex(0);
        toggleUpgradeBlock();
    }

    private void deleteOrder() {
        if (!isOwner() && !isJob()) return;
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите заявку!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Удалить заявку №" + id + "?",
                "Подтверждение", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        JsonDataManager.deleteOrderFromDb(id);
        orders.clear();
        orders.addAll(JsonDataManager.loadData());
        refreshTable();
        refreshMyTable();
        if (statsArea != null) updateStats();
    }

    private void clearAll() {
        if (!isOwner()) return;
        if (orders.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Удалить ВСЕ заявки безвозвратно?",
                "Подтверждение", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        for (order o : orders) JsonDataManager.deleteOrderFromDb(o.id);
        orders.clear();
        orders.addAll(JsonDataManager.loadData());
        refreshTable();
        refreshMyTable();
        if (statsArea != null) updateStats();
    }

    // ===================== ОБНОВЛЕНИЕ ТАБЛИЦ =====================
    private void refreshTable() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        for (order o : orders) {
            String all = (o.id + " " + o.name + " " + o.phone + " " + o.model + " " +
                    o.issue + " " + o.urgency + " " + o.date + " " + o.progress).toLowerCase();
            if (!q.isEmpty() && !all.contains(q)) continue;
            tableModel.addRow(new Object[]{
                    o.id, o.name, o.phone, o.model, o.issue, o.urgency,
                    o.progress + "%", o.date
            });
        }
    }

    // Мои заявки: активные + архивные
    private void refreshMyTable() {
        if (myTableModel == null) return;
        myTableModel.setRowCount(0);

        // Активные заявки
        for (order o : orders) {
            if (o.clientLogin == null || !o.clientLogin.equalsIgnoreCase(currentUser.login)) continue;
            String ratingStr = o.rating > 0 ? o.rating + " / 5" : "—";
            String fb = (o.feedback == null || o.feedback.isEmpty()) ? "—" : o.feedback;
            myTableModel.addRow(new Object[]{
                    o.id, o.model, o.issue, o.progress + "%", o.date, ratingStr, fb
            });
        }

        // Архивные заявки этого клиента
        for (order o : JsonDataManager.loadArchiveByLogin(currentUser.login)) {
            String ratingStr = o.rating > 0 ? o.rating + " / 5" : "—";
            String fb = (o.feedback == null || o.feedback.isEmpty()) ? "—" : o.feedback;
            myTableModel.addRow(new Object[]{
                    o.id, o.model, o.issue, "100% (архив)", o.date, ratingStr, fb
            });
        }
    }

    private void refreshArchive() {
        if (archiveModel == null) return;
        archiveModel.setRowCount(0);
        for (order o : JsonDataManager.loadArchive()) {
            String ratingStr = o.rating > 0 ? o.rating + " / 5" : "—";
            String fb = (o.feedback == null || o.feedback.isEmpty()) ? "—" : o.feedback;
            archiveModel.addRow(new Object[]{
                    o.id, o.name, o.phone, o.model, o.issue,
                    o.urgency, o.date, ratingStr, fb
            });
        }
    }

    // ===================== ДЕТАЛИ: ОБЩИЙ СПИСОК =====================
    private void showDetails() {
        if (table == null) return;
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (Integer) tableModel.getValueAt(row, 0);
        order o = orders.stream().filter(x -> x.id == id).findFirst().orElse(null);
        if (o == null) return;

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JTextArea area = new JTextArea(18, 55);
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setText(
                "Заявка №" + o.id + "\nДата: " + o.date + "\n\n" +
                        "Клиент: " + o.name + "\nТелефон: " + o.phone + "\n" +
                        "Модель: " + o.model + "\nПоломка: " + o.issue + "\n" +
                        "Срочность: " + o.urgency + "\n" +
                        "Клиент-логин: " + (o.clientLogin == null || o.clientLogin.isEmpty() ? "—" : o.clientLogin) + "\n\n" +
                        "Описание:\n" + (o.desc.isEmpty() ? "—" : o.desc) +
                        "\n\n--- Оценка клиента ---\n" +
                        "Оценка: " + (o.rating > 0 ? o.rating + " / 5" : "не оценено") + "\n" +
                        "Комментарий: " + (o.feedback == null || o.feedback.isEmpty() ? "—" : o.feedback));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(620, 360));
        panel.add(sp, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel lblP = new JLabel("Прогресс:");
        JSpinner progressSpin = new JSpinner(new SpinnerNumberModel(o.progress, 0, 100, 5));
        JButton btnDone = new JButton("Готово (100%)");
        boolean canEdit = isOwner() || isJob();
        progressSpin.setEnabled(canEdit);
        btnDone.setEnabled(canEdit);
        progressPanel.add(lblP);
        progressPanel.add(progressSpin);
        progressPanel.add(btnDone);
        btnDone.addActionListener(e -> progressSpin.setValue(100));
        panel.add(progressPanel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Детали заявки №" + o.id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && canEdit) {
            int newProgress = (Integer) progressSpin.getValue();
            if (newProgress != o.progress) {
                o.progress = newProgress;
                if (JsonDataManager.updateProgress(o.id, newProgress)) {

                    if (newProgress >= 100) {
                        if (JsonDataManager.moveToArchive(o)) {
                            JsonDataManager.deleteOrderFromDb(o.id);
                            orders.clear();
                            orders.addAll(JsonDataManager.loadData());
                        }
                    }

                    refreshTable();
                    refreshMyTable();
                    refreshArchive();
                    JOptionPane.showMessageDialog(this,
                            "Прогресс обновлён: " + newProgress + "%" +
                                    (newProgress >= 100 ? "\nЗаявка перенесена в Архив." : ""),
                            "ОК", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    // ===================== ДЕТАЛИ: МОИ ЗАЯВКИ (ищет в Orders + ArchiveDone) =====================
    private void showMyOrderDetails() {
        if (myTable == null) return;
        int row = myTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите заявку!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) myTableModel.getValueAt(row, 0);

        order o = orders.stream().filter(x -> x.id == id).findFirst().orElse(null);
        boolean fromArchive = false;

        if (o == null) {
            o = JsonDataManager.loadArchiveByLogin(currentUser.login)
                    .stream().filter(x -> x.id == id).findFirst().orElse(null);
            fromArchive = (o != null);
        }

        if (o == null) return;

        JTextArea area = new JTextArea(16, 55);
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setText(
                (fromArchive ? "Архивная заявка №" : "Моя заявка №") + o.id + "\nДата: " + o.date + "\n\n" +
                        "Модель: " + o.model + "\nПоломка: " + o.issue + "\n" +
                        "Срочность: " + o.urgency + "\n" +
                        "Прогресс: " + (fromArchive ? "100% (в архиве)" : o.progress + "%") + "\n\n" +
                        "Описание:\n" + (o.desc.isEmpty() ? "—" : o.desc) +
                        "\n\n--- Моя оценка ---\n" +
                        "Оценка: " + (o.rating > 0 ? o.rating + " / 5" : "не оценено") + "\n" +
                        "Комментарий: " + (o.feedback == null || o.feedback.isEmpty() ? "—" : o.feedback));

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(620, 340));
        JOptionPane.showMessageDialog(this, sp,
                "Заявка №" + o.id, JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================== ОЦЕНКА (работает и для архивных) =====================
    private void rateSelectedOrder() {
        if (myTable == null) return;
        int row = myTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите заявку!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) myTableModel.getValueAt(row, 0);

        order o = orders.stream().filter(x -> x.id == id).findFirst().orElse(null);
        boolean fromArchive = false;

        if (o == null) {
            o = JsonDataManager.loadArchiveByLogin(currentUser.login)
                    .stream().filter(x -> x.id == id).findFirst().orElse(null);
            fromArchive = (o != null);
        }

        if (o == null) return;

        if (!fromArchive && o.progress < 100) {
            JOptionPane.showMessageDialog(this,
                    "Оценить можно только заявку с прогрессом 100%.\nСейчас: " + o.progress + "%",
                    "Ещё не готово", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JLabel lblRate = new JLabel("Оценка (1–5):");
        JSpinner starsSpin = new JSpinner(new SpinnerNumberModel(
                o.rating > 0 ? o.rating : 5, 1, 5, 1));

        JLabel lblFb = new JLabel("Комментарий:");
        JTextArea fbArea = new JTextArea(4, 30);
        fbArea.setLineWrap(true);
        fbArea.setWrapStyleWord(true);
        fbArea.setText(o.feedback == null ? "" : o.feedback);
        JScrollPane fbScroll = new JScrollPane(fbArea);

        c.gridx = 0; c.gridy = 0; panel.add(lblRate, c);
        c.gridx = 1; panel.add(starsSpin, c);

        c.gridx = 0; c.gridy = 1; panel.add(lblFb, c);
        c.gridx = 1; c.fill = GridBagConstraints.BOTH;
        panel.add(fbScroll, c);

        int res = JOptionPane.showConfirmDialog(this, panel,
                "Оценить заявку №" + o.id,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        int newRating = (Integer) starsSpin.getValue();
        String newFb = fbArea.getText().trim();

        boolean ok;
        if (fromArchive) {
            ok = JsonDataManager.updateArchiveFeedback(o.id, newRating, newFb);
        } else {
            ok = JsonDataManager.updateFeedback(o.id, newRating, newFb);
        }

        if (ok) {
            o.rating = newRating;
            o.feedback = newFb;
            refreshMyTable();
            JOptionPane.showMessageDialog(this,
                    "Спасибо за оценку! " + newRating + " / 5",
                    "ОК", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ===================== АРХИВ: ДЕТАЛИ =====================
    private void showArchiveDetails() {
        if (archiveTable == null) return;
        int row = archiveTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите заявку!", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) archiveModel.getValueAt(row, 0);
        order o = JsonDataManager.loadArchive().stream()
                .filter(x -> x.id == id).findFirst().orElse(null);
        if (o == null) return;

        JTextArea area = new JTextArea(16, 55);
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setText(
                "Архивная заявка №" + o.id + "\n" +
                        "Дата закрытия: " + o.date + "\n\n" +
                        "Клиент: " + o.name + "\nТелефон: " + o.phone + "\n" +
                        "Модель: " + o.model + "\nПоломка: " + o.issue + "\n" +
                        "Срочность: " + o.urgency + "\n" +
                        "Клиент-логин: " + (o.clientLogin == null || o.clientLogin.isEmpty() ? "—" : o.clientLogin) + "\n\n" +
                        "Описание:\n" + (o.desc.isEmpty() ? "—" : o.desc) +
                        "\n\n--- Оценка клиента ---\n" +
                        "Оценка: " + (o.rating > 0 ? o.rating + " / 5" : "не оценено") + "\n" +
                        "Комментарий: " + (o.feedback == null || o.feedback.isEmpty() ? "—" : o.feedback));

        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(620, 360));
        JOptionPane.showMessageDialog(this, sp,
                "Архив — Заявка №" + o.id, JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================== КАЛЬКУЛЯТОР =====================
    private void calcPrice() {
        if (svcBox == null) return;
        String svc = (String) svcBox.getSelectedItem();
        int qty = (Integer) qtySpin.getValue();
        int disc = (Integer) discSpin.getValue();
        int base = services.getOrDefault(svc, 0);
        double total = base * qty * (1 - disc / 100.0);
        resultLabel.setText(String.format("%s × %d = %.2f руб.  (скидка %d%%)", svc, qty, total, disc));
    }

    // ===================== ПРИБЫЛЬ / УБЫТОК =====================
    private void updateStats() {
        if (statsArea == null) return;

        List<order> allActive = new ArrayList<>(orders);
        List<order> archive = JsonDataManager.loadArchive();

        int totalActive = allActive.size();
        int totalArchive = archive.size();
        int total = totalActive + totalArchive;

        double income = 0;
        for (order o : allActive) {
            int price = services.getOrDefault(o.issue, 0);
            if (price == 0) price = 1000;
            income += price;
        }
        for (order o : archive) {
            int price = services.getOrDefault(o.issue, 0);
            if (price == 0) price = 1000;
            income += price;
        }

        double costs = income * 0.30;
        double profit = income - costs;

        // Средняя оценка из активных + архивных
        double sumR = 0; int cntR = 0;
        for (order o : allActive) if (o.rating > 0) { sumR += o.rating; cntR++; }
        for (order o : archive)   if (o.rating > 0) { sumR += o.rating; cntR++; }
        double avgRating = cntR > 0 ? sumR / cntR : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("ФИНАНСОВЫЙ ОТЧЁТ СТУДИИ\n\n");
        sb.append(String.format("Всего заявок:        %d%n", total));
        sb.append(String.format("  • активных:        %d%n", totalActive));
        sb.append(String.format("  • в архиве:        %d%n%n", totalArchive));

        sb.append("ПРИБЫЛЬ\n");
        sb.append(String.format("Доход:               %,.2f руб.%n", income));
        sb.append(String.format("Расходы (30%%):       %,.2f руб.%n", costs));
        sb.append(String.format("%s%n", profit >= 0
                ? "ПРИБЫЛЬ:             " + String.format("%,.2f руб.", profit)
                : "УБЫТОК:              " + String.format("%,.2f руб.", Math.abs(profit))));

        sb.append("\nКАЧЕСТВО\n");
        if (avgRating > 0) sb.append(String.format("Средняя оценка:      %.2f / 5 (%d оценок)%n", avgRating, cntR));
        else sb.append("Средняя оценка:      нет оценок\n");

        sb.append("\nТОП ПОВРЕЖДЕНИЙ\n");
        Map<String, Integer> count = new HashMap<>();
        for (order o : allActive) count.put(o.issue, count.getOrDefault(o.issue, 0) + 1);
        for (order o : archive)   count.put(o.issue, count.getOrDefault(o.issue, 0) + 1);
        count.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .forEach(e -> sb.append(String.format("   %-25s %d%n", e.getKey(), e.getValue())));

        if (count.isEmpty()) sb.append("   нет данных");

        statsArea.setText(sb.toString());
    }

    // ===================== ТЕМА =====================
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

        if (table != null) {
            table.setBackground(dark ? new Color(34, 37, 45) : Color.WHITE);
            table.setForeground(textColor);
            table.setSelectionBackground(accent);
            table.setSelectionForeground(Color.WHITE);
            table.getTableHeader().setBackground(panelBg);
            table.getTableHeader().setForeground(textColor);
        }
        if (myTable != null) {
            myTable.setBackground(dark ? new Color(34, 37, 45) : Color.WHITE);
            myTable.setForeground(textColor);
            myTable.setSelectionBackground(accent);
            myTable.setSelectionForeground(Color.WHITE);
            myTable.getTableHeader().setBackground(panelBg);
            myTable.getTableHeader().setForeground(textColor);
        }
        if (archiveTable != null) {
            archiveTable.setBackground(dark ? new Color(34, 37, 45) : Color.WHITE);
            archiveTable.setForeground(textColor);
            archiveTable.setSelectionBackground(accent);
            archiveTable.setSelectionForeground(Color.WHITE);
            archiveTable.getTableHeader().setBackground(panelBg);
            archiveTable.getTableHeader().setForeground(textColor);
        }
        if (statsArea != null) {
            statsArea.setBackground(dark ? new Color(34, 37, 45) : Color.WHITE);
            statsArea.setForeground(textColor);
        }
        if (resultLabel != null) {
            resultLabel.setBackground(dark ? new Color(30, 60, 90) : new Color(225, 240, 255));
            resultLabel.setForeground(dark ? new Color(150, 220, 255) : new Color(10, 60, 120));
            resultLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1), new EmptyBorder(20, 20, 20, 20)));
        }
        if (themeBtn != null) {
            themeBtn.setBackground(dark ? new Color(50, 140, 200) : new Color(240, 240, 240));
            themeBtn.setForeground(dark ? Color.WHITE : Color.BLACK);
            themeBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
            themeBtn.setOpaque(true);
        }

        if (upgradePanel != null && upgradePanel.isVisible()) checkCompatibility();
        repaint();
    }

    private List<Component> getAllDescendants(Container root) {
        List<Component> list = new ArrayList<>();
        for (Component c : root.getComponents()) {
            list.add(c);
            if (c instanceof Container) list.addAll(getAllDescendants((Container) c));
        }
        return list;
    }
}