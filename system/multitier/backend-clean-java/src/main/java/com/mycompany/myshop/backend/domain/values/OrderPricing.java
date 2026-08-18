package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

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
