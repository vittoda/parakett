package ecommerce;

public class LineItem {
    private String productId;
    private String name;
    private double unitPrice;
    private int quantity;

    public LineItem(String productId, String name, double unitPrice, int quantity) {
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public double calculateSubtotal() {
        return unitPrice * quantity;
    }

    // Getters
    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}