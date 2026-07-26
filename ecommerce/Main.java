package ecommerce;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        // Create sample line items
        LineItem item1 = new LineItem("P001", "Laptop", 850.00, 1);
        LineItem item2 = new LineItem("P002", "Mouse", 25.00, 2);
        LineItem item3 = new LineItem("P003", "Keyboard", 75.00, 1);

        // Create an order and add line items
        Order order = new Order();
        order.addLineItem(item1);
        order.addLineItem(item2);
        order.addLineItem(item3);

        // Assign a percentage discount strategy (10%)
        PercentageDiscount discountStrategy = new PercentageDiscount(10.0);
        order.setDiscountStrategy(discountStrategy);

        // Calculate and print the order summary
        double subtotal = order.calculateSubtotal();
        double discount = order.calculateDiscount();
        double finalTotal = order.calculateFinalTotal();

        System.out.println("--- Order Summary ---");
        System.out.printf("Subtotal: $%.2f\n", subtotal);
        System.out.printf("Discount Applied (%.0f%%): -$%.2f\n", discountStrategy.getDiscountPercentage(), discount);
        System.out.printf("Final Total: $%.2f\n", finalTotal);
        System.out.println("---------------------");
    }
}