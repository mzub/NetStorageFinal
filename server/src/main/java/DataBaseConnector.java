import java.sql.*;
import java.util.ArrayList;

public class DataBaseConnector {
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private ArrayList<String[]> logins = new ArrayList<>();

    public DataBaseConnector() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:server/usersDB");
            System.out.println("DataBase connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String[]> getLogins() {
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            resultSet = statement.executeQuery("SELECT login, passw FROM users");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            while (resultSet.next()) {
                logins.add(new String[]{resultSet.getString("login"), resultSet.getString("passw")});
            }
        } catch (SQLException e) {
                e.printStackTrace();
        }
        return logins;
    }

}
