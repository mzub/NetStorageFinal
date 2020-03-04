import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientGUI {

    JList<String> userPane = new JList<>();
    JList<String> serverPane = new JList<>();

    JFrame frame = new JFrame();
    AuthWindow authWindow;
    JButton buttonCopy = new JButton("Копировать");
    JButton buttonMove = new JButton("Переместить");
    JButton buttonDelete = new JButton("Удалить");
    JButton buttonExit = new JButton("Выход");

    public ClientGUI() throws IOException {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        JPanel leftPanel = new JPanel(new GridBagLayout());
        JPanel rightPanel = new JPanel(new GridBagLayout());
        JLabel userPaneTitle = new JLabel("Файлы на компьютере");
        JLabel serverPaneTitle = new JLabel("Файлы на сервере");

        userPane.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                serverPane.clearSelection();
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        serverPane.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                userPane.clearSelection();
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        panel.add(leftPanel);
        panel.add(rightPanel);

        userPaneTitle.setHorizontalAlignment(SwingConstants.CENTER);
        serverPaneTitle.setHorizontalAlignment(SwingConstants.CENTER);
        userPane.setListData(Files.list(Paths.get("client/repository/")).map((Path path) -> path.getFileName().toString()).sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new));

        GridBagConstraints c = new GridBagConstraints();
        /****************Левая Панель*****************/
        //Заголовок панели клиента
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.ipady = 10;
        leftPanel.add(userPaneTitle, c);
        //Панель клиента
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 1;
        c.insets = new Insets(2,5,5,2);
        c.ipady = 0;
        leftPanel.add(userPane, c);
        //Кнопка переместить
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(0,0,0,0);
        leftPanel.add(buttonMove, c);
        //Кнопка копировать
        c.gridx = 1;
        c.gridy = 2;
        leftPanel.add(buttonCopy, c);
        /****************Правая Панель*****************/
        //Заголовок панели сервера
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.ipady = 10;
        rightPanel.add(serverPaneTitle, c);
        //Панель сервера
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(2,2,5,5);
        c.ipady = 0;
        rightPanel.add(serverPane, c);
        //Кнопка удалить
        c.insets = new Insets(0,0,0,0);
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridy = 2;
        rightPanel.add(buttonDelete, c);
        //Кнопка выход
        c.gridx = 1;
        c.gridy = 2;
        rightPanel.add(buttonExit, c);

        frame.add(panel);
        frame.setTitle("NetStorageZZz");
        frame.setMinimumSize(new Dimension(900, 500));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        authWindow = new AuthWindow(this);
    }

    public JList<String> getUserPane() {
        return userPane;
    }

    public JList getServerPane() {
        return serverPane;
    }

    public JButton getButtonCopy() {
        return buttonCopy;
    }

    public JButton getButtonMove() {
        return buttonMove;
    }

    public JButton getButtonDelete() {
        return buttonDelete;
    }

    public JButton getButtonExit() {
        return buttonExit;
    }

    public JFrame getFrame() {
        return frame;
    }

    public AuthWindow getAuthWindow() {
        return authWindow;
    }
}
