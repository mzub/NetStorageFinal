
import javax.swing.*;
import java.awt.*;

public class AuthWindow extends JDialog {

    private JPanel panel;
    private JLabel loginLabel;
    private JLabel passwordLabel;
    private JTextField loginInputField;
    private JPasswordField passwordInputField;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JCheckBox checkBox;

    private String login;
    private char[] password;
    private boolean shouldBeSaved;


    public JTextField getLoginInputField() {
        return loginInputField;
    }

    public JPasswordField getPasswordInputField() {
        return passwordInputField;
    }

    public JButton getButtonOk() {
        return buttonOk;
    }

    public JButton getButtonCancel() {
        return buttonCancel;
    }


    public AuthWindow(ClientGUI clientGUI) {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        loginLabel  = new JLabel("Login:");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10, 10, 5, 5);
        panel.add(loginLabel, c);

        passwordLabel  = new JLabel("Password:");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(5, 10, 5, 5);
        panel.add(passwordLabel, c);

        loginInputField  = new JTextField( 12);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(10, 5, 5, 10);
        panel.add(loginInputField, c);

        passwordInputField  = new JPasswordField( 12);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(5, 5, 5, 10);
        panel.add(passwordInputField, c);

        buttonOk = new JButton("Авторизоваться");
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.insets = new Insets(5, 10, 10, 5);
        panel.add(buttonOk, c);

        buttonCancel = new JButton("Закрыть");
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.insets = new Insets(5, 5, 10, 10);
        panel.add(buttonCancel, c);

        this.add(panel);

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(clientGUI.getFrame());
        this.setVisible(true);
    }


    public String getLogin() {
        return loginInputField.getText();
    }

    public char[] getPassword() {
        return passwordInputField.getPassword();
    }

    public boolean isShouldBeSaved() {
        return shouldBeSaved;
    }

}
