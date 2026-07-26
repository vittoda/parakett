package ecommerce;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private List<LineItem> lineItems;
    private DiscountStrategy discountStrategy;

    public Order() {
        this.lineItems = new ArrayList<>();
    }

    public void addLineItem(LineItem item) {
        this.lineItems.add(item);
    }

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }

    public double calculateSubtotal() {
        double subtotal = 0;
        for (LineItem item : lineItems) {
            subtotal += item.calculateSubtotal();
        }
        return subtotal;
    }

    public double calculateDiscount() {
        if (discountStrategy != null) {
            return discountStrategy.applyDiscount(calculateSubtotal());
        }
        return 0;
    }

    public double calculateFinalTotal() {
        return calculateSubtotal() - calculateDiscount();
    }
}