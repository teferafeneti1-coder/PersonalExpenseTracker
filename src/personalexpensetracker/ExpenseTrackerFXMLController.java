package personalexpensetracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.io.*;

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
    @FXML private PieChart incomePieChart;
    @FXML private PieChart expensePieChart;

    private String currentUsername;
    private int currentUserId;
    private Stage loginStage;
    private ObservableList<Expense> data = FXCollections.observableArrayList();

    public void setUser(String username, int userId, Stage loginStage) {
        this.currentUsername = username;
        this.currentUserId = userId;
        this.loginStage = loginStage;
        welcomeLabel.setText("Welcome, " + username);
        loadData();
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

        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) loadExpenseIntoForm(newVal);
        });
    }

    private void loadExpenseIntoForm(Expense e) {
        datePicker.setValue(LocalDate.parse(e.getDate()));
        descField.setText(e.getDescription());
        categoryCombo.setValue(e.getCategory());
        typeCombo.setValue(e.getType());
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
                data.add(new Expense(
                    rs.getInt("id"),
                    rs.getString("date_str"),
                    rs.getString("description"),
                    rs.getString("category"),
                    rs.getDouble("amount"),
                    rs.getString("t_type")
                ));
            }

            updateSummary();
            updateCharts();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error loading data: " + e.getMessage());
        }
    }

    @FXML
    private void addExpense() {
        if (!validateForm()) return;
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
            showAlert("Please select an entry to edit");
            return;
        }
        if (!validateForm()) return;

        double amount = parseAmount();
        if (Double.isNaN(amount)) return;

        saveTransaction(selected.getId(), amount);
        clearForm();
        loadData();
        showAlert("Entry updated successfully");
    }

    @FXML
    private void deleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an entry to delete");
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
    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Transactions as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(expenseTable.getScene().getWindow());

        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Date,Description,Category,Type,Amount\n");

            for (Expense e : data) {
                String desc = e.getDescription().replace("\"", "\"\"");
                writer.write(String.format("%s,\"%s\",%s,%s,%.2f\n",
                    e.getDate(), desc, e.getCategory(), e.getType(), e.getAmount()));
            }
            showAlert("Data exported successfully to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert("Export failed: " + ex.getMessage());
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

    private boolean validateForm() {
        if (datePicker.getValue() == null || descField.getText().trim().isEmpty() ||
            categoryCombo.getValue() == null || typeCombo.getValue() == null ||
            amountField.getText().trim().isEmpty()) {
            showAlert("Please fill all fields");
            return false;
        }
        return true;
    }

    private double parseAmount() {
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if ("Expense".equals(typeCombo.getValue())) {
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
            sql = "INSERT INTO transactions (user_id, t_date, description, category, t_type, amount) VALUES (?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE transactions SET t_date=?, description=?, category=?, t_type=?, amount=? WHERE id=?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(id == null ? "Error adding entry" : "Error updating entry");
        }
    }

    private void clearForm() {
        datePicker.setValue(LocalDate.now());
        descField.clear();
        amountField.clear();
        categoryCombo.setValue(null);
        typeCombo.setValue(null);
    }

    private void updateSummary() {
    double income = 0;
    double expense = 0;

    for (Expense e : data) {
        double amt = e.getAmount();
        if (amt > 0) income += amt;
        else expense += Math.abs(amt);
    }

    System.out.println("Income: " + income);
    System.out.println("Expense: " + expense);
    System.out.println("Balance: " + (income - expense));

    incomeLabel.setText(String.format("Total Income: %.2f ETB", income));
    expenseLabel.setText(String.format("Total Expenses: %.2f ETB", expense));

    double balance = income - expense;
    balanceLabel.setText(String.format("Net Balance: %.2f ETB", balance));

    // Force visible + strong contrast for testing
    balanceLabel.setStyle(
        "-fx-font-size: 24; " +
        "-fx-font-weight: bold; " +
        "-fx-text-fill: #000000; " +           // black text
        "-fx-background-color: #FFFF99; " +    // light yellow background
        "-fx-padding: 8;"                      // more space
    );

    System.out.println("balanceLabel text set to: " + balanceLabel.getText());
}

    private void updateCharts() {
        Map<String, Double> incomeMap = new HashMap<>();
        Map<String, Double> expenseMap = new HashMap<>();

        for (Expense e : data) {
            String cat = e.getCategory();
            double amt = e.getAmount();
            if (amt > 0) {
                incomeMap.merge(cat, amt, Double::sum);
            } else {
                expenseMap.merge(cat, Math.abs(amt), Double::sum);
            }
        }

        // Income Pie
        ObservableList<PieChart.Data> incomeData = FXCollections.observableArrayList();
        incomeMap.forEach((cat, value) -> incomeData.add(new PieChart.Data(cat + " (" + String.format("%.0f", value) + ")", value)));
        incomePieChart.setData(incomeData);

        // Expense Pie
        ObservableList<PieChart.Data> expenseData = FXCollections.observableArrayList();
        expenseMap.forEach((cat, value) -> expenseData.add(new PieChart.Data(cat + " (" + String.format("%.0f", value) + ")", value)));
        expensePieChart.setData(expenseData);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}