import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginWindow lw = new LoginWindow();
            lw.setVisible(true);

            lw.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    User u = lw.getAuthenticatedUser();
                    if (u != null) new PCRepairStudio(u).setVisible(true);
                    else System.exit(0);
                }
            });
        });
    }
}