package ecommerce;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create sample line items
        LineItem laptop = new LineItem("P001", "Laptop", 850.00, 1);
        LineItem mouse = new LineItem("P002", "Mouse", 25.00, 2);
        LineItem keyboard = new LineItem("P003", "Keyboard", 75.00, 1);

        // Add line items to a list
        List<LineItem> items = new ArrayList<>();
        items.add(laptop);
        items.add(mouse);
        items.add(keyboard);

        // Create an order and assign a discount strategy
        Order order = new Order(items, new PercentageDiscount(10.0));

        // Calculate values
        double subtotal = order.calculateSubtotal();
        double discount = order.calculateDiscount();
        double finalTotal = order.calculateFinalTotal();

        // Print the order summary
        System.out.println("--- Order Summary ---");
        System.out.printf("Subtotal: $%.2f\n", subtotal);
        System.out.printf("Discount Applied (10%%): -$%.2f\n", discount);
        System.out.printf("Final Total: $%.2f\n", finalTotal);
        System.out.println("---------------------");
    }
}