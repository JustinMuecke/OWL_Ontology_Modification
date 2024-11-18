package database;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PostgresDB {

    private final String POSTGRES_HOST;
    private final String POSTGRES_DB;
    private final String POSTGRES_USER;
    private final String POSTGRES_PASSWORD;

    private final int PORT;

    private final String URL;
    public PostgresDB(String host, String db, String user, String password, int port) throws SQLException {
        this.POSTGRES_HOST = host;
        this.POSTGRES_DB = db;
        this.POSTGRES_USER = user;
        this.POSTGRES_PASSWORD = password;
        this.PORT = port;
        this.URL = "jdbc:postgresql://" + this.POSTGRES_HOST + ":" + this.PORT +"/" + this.POSTGRES_DB;
    }


    public void updateStatusInModificationDatabaseStart(String fileName) {
        String updateQuery = "UPDATE modification SET status = ?, injected_axiom=? WHERE file_name = ?";

        try (java.sql.Connection dbConnection = DriverManager.getConnection(this.URL, this.POSTGRES_USER, this.POSTGRES_PASSWORD);
             PreparedStatement statement = dbConnection.prepareStatement(updateQuery)) {
            statement.setString(1, "Processing");
            statement.setString(2, "TBD");
            statement.setString(3, fileName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error logging to the database");
        }
    }


    public void updateStatusInModificationDatabaseEnd(String fileName, String injectedAxiom) {
        String updateQuery = "UPDATE modification SET status = ?, injected_axiom=? WHERE file_name = ?";

        try (java.sql.Connection dbConnection = DriverManager.getConnection(this.URL, this.POSTGRES_USER, this.POSTGRES_PASSWORD);
             PreparedStatement statement = dbConnection.prepareStatement(updateQuery)) {
            statement.setString(1, "Done");
            statement.setString(2, injectedAxiom);
            statement.setString(3, fileName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error logging to the database");
        }
    }

    public void updasteStatusInPreprocessingDatabase(String fileName){
        String insertQuery = "INSERT INTO preprocessing (file_name, status, consistent) VALUES (?, ?, ?)";
        try (java.sql.Connection dbConnection = DriverManager.getConnection(this.URL, this.POSTGRES_USER, this.POSTGRES_PASSWORD);
             PreparedStatement statement = dbConnection.prepareStatement(insertQuery)) {
            statement.setString(1, fileName);
            statement.setString(2, "Waiting");
            statement.setString(3, "False");
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error logging to the database");
        }
    }
    public void updateStatusInModificationDatabaseEndError(String fileName) {
        String updateQuery = "UPDATE modification SET status = ?, error_message=? WHERE file_name = ?";

        try (java.sql.Connection dbConnection = DriverManager.getConnection(this.URL, this.POSTGRES_USER, this.POSTGRES_PASSWORD);
             PreparedStatement statement = dbConnection.prepareStatement(updateQuery)) {
            statement.setString(1, "Failed");
            statement.setString(2, "No injectable Axiom found");
            statement.setString(3, fileName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error logging to the database");
        }
    }
}
