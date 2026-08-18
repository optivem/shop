package com.mycompany.myshop.backend.domain.pricing;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;

/**
 * What an order costs and how it got there. {@link #price} is the whole pricing chain — the eight
 * loose {@code BigDecimal} locals that used to live in {@code placeOrder}, expressed once as typed
 * arithmetic.
 *
 * <p>Every component is rounded to its recorded scale here, in the canonical constructor, from
 * exact intermediates — so the promoted price feeding the discount, and the discounted subtotal
 * feeding the tax, are never pre-rounded. See {@link Money#rounded()}.
 */
public record OrderPricing(
        Money unitPrice,
        int quantity,
        Money basePrice,
        Rate discountRate,
        Money discountAmount,
        Money subtotalPrice,
        Rate taxRate,
        Money taxAmount,
        Money totalPrice) {

    public OrderPricing {
        Guard.notNull(unitPrice, "unitPrice");
        Guard.notNull(basePrice, "basePrice");
        Guard.notNull(discountRate, "discountRate");
        Guard.notNull(discountAmount, "discountAmount");
        Guard.notNull(subtotalPrice, "subtotalPrice");
        Guard.notNull(taxRate, "taxRate");
        Guard.notNull(taxAmount, "taxAmount");
        Guard.notNull(totalPrice, "totalPrice");

        unitPrice = unitPrice.rounded();
        basePrice = basePrice.rounded();
        discountRate = discountRate.rounded();
        discountAmount = discountAmount.rounded();
        subtotalPrice = subtotalPrice.rounded();
        taxRate = taxRate.rounded();
        taxAmount = taxAmount.rounded();
        totalPrice = totalPrice.rounded();
    }

    /**
     * Prices a line: quantity at the unit price, reduced by the promotion, then by the coupon, then
     * taxed. The base price is recorded before the promotion is applied, matching the schema.
     */
    public static OrderPricing price(Money unitPrice, int quantity, Rate promotionFactor,
                                     Rate discountRate, Rate taxRate) {
        var basePrice = unitPrice.times(quantity);
        var promotedPrice = basePrice.applyRate(promotionFactor);
        var discountAmount = promotedPrice.applyRate(discountRate);
        var subtotalPrice = promotedPrice.minus(discountAmount);
        var taxAmount = subtotalPrice.applyRate(taxRate);
        var totalPrice = subtotalPrice.plus(taxAmount);

        return new OrderPricing(unitPrice, quantity, basePrice, discountRate, discountAmount,
                subtotalPrice, taxRate, taxAmount, totalPrice);
    }
}
