package ecommerce;

public class PercentageDiscount implements DiscountStrategy {
    private double discountPercentage;

    public PercentageDiscount(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    @Override
    public double applyDiscount(double totalAmount) {
        if (totalAmount > 100) {
            return totalAmount * (discountPercentage / 100);
        }
        return 0;
    }
}