package personalexpensetracker;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private Stage loginStage;

    public void setLoginStage(Stage stage) {
        this.loginStage = stage;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter username and password");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                openMainWindow(username, userId);
                loginStage.hide();
            } else {
                statusLabel.setText("Invalid username or password");
            }
        } catch (Exception e) {
            statusLabel.setText("Database error");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            statusLabel.setText("Fill all fields");
            return;
        }

        if (!password.equals(confirm)) {
            statusLabel.setText("Passwords do not match");
            return;
        }

        if (password.length() < 4) {
            statusLabel.setText("Password too short (min 4 chars)");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String check = "SELECT id FROM users WHERE username = ?";
            PreparedStatement psCheck = conn.prepareStatement(check);
            psCheck.setString(1, username);
            if (psCheck.executeQuery().next()) {
                statusLabel.setText("Username already exists");
                return;
            }

            String insert = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement psInsert = conn.prepareStatement(insert);
            psInsert.setString(1, username);
            psInsert.setString(2, password);
            psInsert.executeUpdate();

            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Registered successfully! Now click Login.");
            confirmPasswordField.clear();
        } catch (Exception e) {
            statusLabel.setText("Error during registration");
            e.printStackTrace();
        }
    }

    private void openMainWindow(String username, int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ExpenseTrackerFXML.fxml"));
            Parent root = loader.load();

            ExpenseTrackerFXMLController controller = loader.getController();
            controller.setUser(username, userId, loginStage);
            controller.loadData();

            Stage mainStage = new Stage();
            mainStage.setTitle("Tracker - " + username);
            mainStage.setScene(new Scene(root, 900, 650));
            mainStage.show();

            mainStage.setOnCloseRequest(e -> loginStage.show());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}