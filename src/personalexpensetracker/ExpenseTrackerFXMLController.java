package personalexpensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ExpenseTrackerFXMLController implements Initializable {

    @FXML private DatePicker datePicker;
    @FXML private TextField descField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField amountField;
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> dateCol;
    @FXML private TableColumn<Expense, String> descCol;
    @FXML private TableColumn<Expense, String> categoryCol;
    @FXML private TableColumn<Expense, String> typeCol;
    @FXML private TableColumn<Expense, Number> amountCol;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private Label balanceLabel;
    @FXML private Label welcomeLabel;

    private String currentUsername;
    private int currentUserId;
    private Stage loginStage;

    private ObservableList<Expense> data = FXCollections.observableArrayList();

    public void setUser(String username, int userId, Stage loginStage) {
        this.currentUsername = username;
        this.currentUserId = userId;
        this.loginStage = loginStage;

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dateCol.setCellValueFactory(cell -> cell.getValue().dateProperty());
        descCol.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        categoryCol.setCellValueFactory(cell -> cell.getValue().categoryProperty());
        typeCol.setCellValueFactory(cell -> cell.getValue().typeProperty());
        amountCol.setCellValueFactory(cell -> cell.getValue().amountProperty());

        expenseTable.setItems(data);

        categoryCombo.getItems().addAll("Salary", "Food", "Transport", "Rent", "Freelance", "Other");
        typeCombo.getItems().addAll("Income", "Expense");

        datePicker.setValue(LocalDate.now());

        // Optional UX improvement: enable/disable edit & delete when row selected
        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            // You can add @FXML private Button editButton; and deleteButton; if you want
            // editButton.setDisable(!hasSelection);
            // deleteButton.setDisable(!hasSelection);
        });

        // Optional: double-click row to load into form for editing
        expenseTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Expense selected = expenseTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadExpenseIntoForm(selected);
                }
            }
        });
    }

    private void loadExpenseIntoForm(Expense e) {
        datePicker.setValue(LocalDate.parse(e.getDate()));
        descField.setText(e.getDescription());
        categoryCombo.setValue(e.getCategory());
        typeCombo.setValue(e.getType());
        // Show positive amount in field (we negate on save if Expense)
        amountField.setText(String.format("%.2f", Math.abs(e.getAmount())));
    }

    public void loadData() {
        data.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT id, t_date::text AS date_str, description, category, t_type, amount
                FROM transactions
                WHERE user_id = ?
                ORDER BY t_date DESC, created_at DESC
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Expense e = new Expense(
                    rs.getInt("id"),
                    rs.getString("date_str"),
                    rs.getString("description"),
                    rs.getString("category"),
                    rs.getDouble("amount"),
                    rs.getString("t_type")
                );
                data.add(e);
            }
            updateSummary();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error loading data");
        }
    }

    @FXML
    private void addExpense() {
        if (!isFormValid()) return;

        double amount = parseAmount();
        if (Double.isNaN(amount)) return;

        saveTransaction(null, amount);
        clearForm();
        loadData();
    }

    @FXML
    private void editExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a row to edit");
            return;
        }

        if (!isFormValid()) return;

        double amount = parseAmount();
        if (Double.isNaN(amount)) return;

        saveTransaction(selected.getId(), amount);
        clearForm();
        loadData();
        showAlert("Entry updated successfully");
    }

    private boolean isFormValid() {
        if (datePicker.getValue() == null || descField.getText().isBlank() ||
            categoryCombo.getValue() == null || typeCombo.getValue() == null ||
            amountField.getText().isBlank()) {
            showAlert("Please fill all fields");
            return false;
        }
        return true;
    }

    private double parseAmount() {
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (typeCombo.getValue().equals("Expense")) {
                amount = -amount;
            }
            return amount;
        } catch (NumberFormatException e) {
            showAlert("Amount must be a valid number");
            return Double.NaN;
        }
    }

    private void saveTransaction(Integer id, double amount) {
        String sql;
        if (id == null) {
            sql = """
                INSERT INTO transactions (user_id, t_date, description, category, t_type, amount)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        } else {
            sql = """
                UPDATE transactions
                SET t_date = ?, description = ?, category = ?, t_type = ?, amount = ?
                WHERE id = ?
                """;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(datePicker.getValue()));
            ps.setString(2, descField.getText().trim());
            ps.setString(3, categoryCombo.getValue());
            ps.setString(4, typeCombo.getValue());
            ps.setDouble(5, amount);

            if (id != null) {
                ps.setInt(6, id);
            } else {
                ps.setInt(6, currentUserId);  // for INSERT → user_id is position 1
                // Wait — adjust indices for INSERT
                // Actually better to separate logic slightly:
            }

            // Correction: separate insert vs update
            if (id == null) {
                ps.setInt(1, currentUserId);
                ps.setDate(2, java.sql.Date.valueOf(datePicker.getValue()));
                ps.setString(3, descField.getText().trim());
                ps.setString(4, categoryCombo.getValue());
                ps.setString(5, typeCombo.getValue());
                ps.setDouble(6, amount);
            } else {
                ps.setDate(1, java.sql.Date.valueOf(datePicker.getValue()));
                ps.setString(2, descField.getText().trim());
                ps.setString(3, categoryCombo.getValue());
                ps.setString(4, typeCombo.getValue());
                ps.setDouble(5, amount);
                ps.setInt(6, id);
            }

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(id == null ? "Error saving entry" : "Error updating entry");
        }
    }

    private void clearForm() {
        datePicker.setValue(LocalDate.now());
        descField.clear();
        amountField.clear();
        categoryCombo.setValue(null);
        typeCombo.setValue(null);
    }

    @FXML
    private void deleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a row to delete");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM transactions WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, selected.getId());
            ps.executeUpdate();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error deleting entry");
        }
    }

    @FXML
    private void handleLogout() {
        Stage mainStage = (Stage) expenseTable.getScene().getWindow();
        mainStage.close();
        if (loginStage != null) {
            loginStage.show();
        }
    }

    private void updateSummary() {
        double income = 0;
        double expense = 0;
        for (Expense e : data) {
            double a = e.getAmount();
            if (a > 0) income += a;
            else expense += Math.abs(a);
        }

        incomeLabel.setText(String.format("Total Income: %.2f", income));
        expenseLabel.setText(String.format("Total Expenses: %.2f", expense));

        double bal = income - expense;
        balanceLabel.setText(String.format("Net Balance: %.2f", bal));

        if (bal >= 0) {
            balanceLabel.setStyle("-fx-text-fill: green; -fx-font-size: 18;");
        } else {
            balanceLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18;");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}