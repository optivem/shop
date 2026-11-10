package com.mycompany.myshop.controllers.web.storefront;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Money display: one currency for every shipping country, with thousands separators and 2 decimals. */
public final class Money {

    private Money() {
    }

    /** 1299.99 -> "$1,299.99". */
    public static String format(BigDecimal amount) {
        var format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        var value = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        return "$" + format.format(value);
    }

    /** unit price x quantity. */
    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Lets the templates format money: {@code ${money.format(product.price)}}. */
    public static final class Formatter {
        public static final Formatter INSTANCE = new Formatter();

        public String format(BigDecimal amount) {
            return Money.format(amount);
        }

        public String lineTotal(BigDecimal unitPrice, int quantity) {
            return Money.format(Money.lineTotal(unitPrice, quantity));
        }
    }
}
