import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginWindow extends JFrame {

    private JTextField loginField;
    private JPasswordField passField;
    private User authenticatedUser = null;

    public LoginWindow() {
        setTitle("Вход в систему — Студия ремонта ПК");
        setSize(420, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        root.setBackground(new Color(245, 247, 250));

        JLabel title = new JLabel("Студия ремонта ПК");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblLogin = new JLabel("Логин:");
        lblLogin.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loginField = new JTextField(15);
        loginField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblPass = new JLabel("Пароль:");
        lblPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passField = new JPasswordField(15);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        c.gridx = 0; c.gridy = 0; c.weightx = 0; form.add(lblLogin, c);
        c.gridx = 1; c.weightx = 1; form.add(loginField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; form.add(lblPass, c);
        c.gridx = 1; c.weightx = 1; form.add(passField, c);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton btnLogin = new JButton("Войти");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setBackground(new Color(46, 125, 50));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(true);

        JButton btnRegister = new JButton("Регистрация");
        btnRegister.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRegister.setFocusPainted(false);

        buttons.add(btnRegister);
        buttons.add(btnLogin);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);

        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> openRegisterDialog());
        passField.addActionListener(e -> doLogin());
    }

    private void doLogin() {
        String login = loginField.getText().trim();
        String pass = new String(passField.getPassword());

        if (login.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите логин и пароль!",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User u = AuthManager.authenticate(login, pass);
        if (u == null) {
            JOptionPane.showMessageDialog(this, "Неверный логин или пароль!",
                    "Ошибка входа", JOptionPane.ERROR_MESSAGE);
            return;
        }

        authenticatedUser = u;
        dispose();
    }

    private void openRegisterDialog() {
        JTextField l = new JTextField();
        JPasswordField p = new JPasswordField();
        JTextField f = new JTextField();
        JComboBox<String> r = new JComboBox<>(new String[]{"user", "job", "owner"});

        Object[] msg = {
                "Логин:", l,
                "Пароль (мин. 4 символа):", p,
                "ФИО:", f,
                "Роль:", r
        };

        int res = JOptionPane.showConfirmDialog(this, msg,
                "Регистрация нового пользователя",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res != JOptionPane.OK_OPTION) return;

        boolean ok = AuthManager.register(
                l.getText().trim(),
                new String(p.getPassword()),
                f.getText().trim().isEmpty() ? l.getText().trim() : f.getText().trim(),
                (String) r.getSelectedItem()
        );

        if (ok) {
            JOptionPane.showMessageDialog(this, "Пользователь создан!",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Не удалось создать: логин занят или пароль < 4 символов.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}