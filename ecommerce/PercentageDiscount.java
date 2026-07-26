package ecommerce;

public class PercentageDiscount implements DiscountStrategy {
    private final double percentage;

    public PercentageDiscount(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public double applyDiscount(double totalAmount) {
        if (totalAmount > 100) {
            return totalAmount * (percentage / 100);
        }
        return 0;
    }
}