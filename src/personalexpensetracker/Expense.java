package personalexpensetracker;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class Expense {

    private int id;   // database primary key

    private final SimpleStringProperty date;
    private final SimpleStringProperty description;
    private final SimpleStringProperty category;
    private final SimpleStringProperty type;
    private final SimpleDoubleProperty amount;

    public Expense(int id, String date, String description, String category, double amount, String type) {
        this.id = id;
        this.date = new SimpleStringProperty(date);
        this.description = new SimpleStringProperty(description);
        this.category = new SimpleStringProperty(category);
        this.type = new SimpleStringProperty(type);
        this.amount = new SimpleDoubleProperty(amount);
    }

    public int getId() {
        return id;
    }

    public String getDate() { return date.get(); }
    public String getDescription() { return description.get(); }
    public String getCategory() { return category.get(); }
    public String getType() { return type.get(); }
    public double getAmount() { return amount.get(); }

    public SimpleStringProperty dateProperty() { return date; }
    public SimpleStringProperty descriptionProperty() { return description; }
    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleStringProperty typeProperty() { return type; }
    public SimpleDoubleProperty amountProperty() { return amount; }
}