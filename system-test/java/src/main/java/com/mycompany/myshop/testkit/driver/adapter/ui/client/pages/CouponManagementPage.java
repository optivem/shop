package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CouponManagementPage extends BasePage {
    private static final String COUPON_CODE_INPUT_SELECTOR = "[aria-label=\"Coupon Code\"]";
    private static final String DISCOUNT_RATE_INPUT_SELECTOR = "[aria-label=\"Discount Rate\"]";
    private static final String VALID_FROM_INPUT_SELECTOR = "[aria-label=\"Valid From\"]";
    private static final String VALID_TO_INPUT_SELECTOR = "[aria-label=\"Valid To\"]";
    private static final String USAGE_LIMIT_INPUT_SELECTOR = "[aria-label=\"Usage Limit\"]";
    private static final String PUBLISH_COUPON_BUTTON_SELECTOR = "[aria-label=\"Create Coupon\"]";

    // Selectors for browsing coupons
    private static final String COUPONS_TABLE_SELECTOR = "[aria-label=\"Coupons Table\"]";

    // Time format constants
    private static final String TIME_MIDNIGHT = "T00:00";
    private static final String TIME_END_OF_DAY = "T23:59";

    // Table selectors
    private static final String TABLE_CELL_CODE_SELECTOR = "table.table tbody tr td:nth-child(1)";
    private static final String TABLE_CELL_DISCOUNT_SELECTOR = "table.table tbody tr td:nth-child(2)";
    private static final String TABLE_CELL_VALID_FROM_SELECTOR = "table.table tbody tr td:nth-child(3)";
    private static final String TABLE_CELL_VALID_TO_SELECTOR = "table.table tbody tr td:nth-child(4)";
    private static final String TABLE_CELL_USAGE_LIMIT_SELECTOR = "table.table tbody tr td:nth-child(5)";
    private static final String TABLE_CELL_USED_COUNT_SELECTOR = "table.table tbody tr td:nth-child(6)";

    // Display text constants
    private static final String PERCENT_SYMBOL = "%";
    private static final String TEXT_IMMEDIATE = "Immediate";
    private static final String TEXT_NEVER = "Never";
    private static final String TEXT_UNLIMITED = "Unlimited";

    public CouponManagementPage(PageClient pageClient) {
        super(pageClient);
    }

    public void inputCouponCode(String couponCode) {
        pageClient.fill(COUPON_CODE_INPUT_SELECTOR, couponCode);
    }

    public void inputDiscountRate(String discountRate) {
        pageClient.fill(DISCOUNT_RATE_INPUT_SELECTOR, discountRate);
    }

    public void inputValidFrom(String validFrom) {
        var datetimeValue = getValidFromDateTimeString(validFrom);
        pageClient.fill(VALID_FROM_INPUT_SELECTOR, datetimeValue);
    }

    public void inputValidTo(String validTo) {
        var datetimeValue = getValidToDateTimeString(validTo);
        pageClient.fill(VALID_TO_INPUT_SELECTOR, datetimeValue);
    }

    public void inputUsageLimit(String usageLimit) {
        pageClient.fill(USAGE_LIMIT_INPUT_SELECTOR, usageLimit);
    }

    public void clickPublishCoupon() {
        pageClient.click(PUBLISH_COUPON_BUTTON_SELECTOR);
    }

    public List<BrowseCouponsResponse.CouponDto> readCoupons() {
        // Wait for table to appear/refresh after operations like PublishCoupon
        pageClient.waitForVisible(COUPONS_TABLE_SELECTOR);

        var coupons = new ArrayList<BrowseCouponsResponse.CouponDto>();
        // Use readAllTextContentsWithoutWait to avoid strict mode violations
        // These selectors intentionally match multiple elements (one per table row)
        var codes = pageClient.readAllTextContentsNoWait(TABLE_CELL_CODE_SELECTOR);

        // If no codes found, table is empty
        if (codes.isEmpty()) {
            return new ArrayList<>();
        }

        var discountRates = pageClient.readAllTextContents(TABLE_CELL_DISCOUNT_SELECTOR);
        var validFroms = pageClient.readAllTextContents(TABLE_CELL_VALID_FROM_SELECTOR);
        var validTos = pageClient.readAllTextContents(TABLE_CELL_VALID_TO_SELECTOR);
        var usageLimits = pageClient.readAllTextContents(TABLE_CELL_USAGE_LIMIT_SELECTOR);
        var usedCounts = pageClient.readAllTextContents(TABLE_CELL_USED_COUNT_SELECTOR);

        var rowCount = codes.size();

        // Double-check we have data before trying to access it
        // Also verify all columns have the same row count (handles empty tables or malformed data)
        if (rowCount == 0 || discountRates.size() != rowCount || validFroms.size() != rowCount
                || validTos.size() != rowCount || usageLimits.size() != rowCount || usedCounts.size() != rowCount) {
            return new ArrayList<>();
        }

        // Build coupon objects from the collected data
        for (int i = 0; i < rowCount; i++) {
            var code = codes.get(i).trim();
            var discountRateText = discountRates.get(i).trim().replace(PERCENT_SYMBOL, "");
            var validFromText = validFroms.get(i).trim();
            var validToText = validTos.get(i).trim();
            var usageLimitText = usageLimits.get(i).trim();
            var usedCountText = usedCounts.get(i).trim();

            var coupon = BrowseCouponsResponse.CouponDto.builder()
                    .code(code)
                    .discountRate(parseDiscountRate(discountRateText))
                    .validFrom(toInstant(validFromText))
                    .validTo(toInstant(validToText))
                    .usageLimit(parseUsageLimit(usageLimitText))
                    .usedCount(Converter.toInteger(usedCountText))
                    .build();

            coupons.add(coupon);
        }

        return coupons;
    }

    private static String getValidFromDateTimeString(String validFrom) {
        if(validFrom == null || validFrom.isEmpty()) {
            return "";
        }

        String dateOnly = validFrom.substring(0, 10);
        return dateOnly + TIME_MIDNIGHT;
    }

    private static String getValidToDateTimeString(String validTo) {
        if(validTo == null || validTo.isEmpty()) {
            return "";
        }

        String dateOnly = validTo.substring(0, 10);
        return dateOnly + TIME_END_OF_DAY;
    }

    /**
     * Percent text scraped from the table (e.g. {@code "15"}) to a rate ({@code 0.15}).
     *
     * <p>{@code movePointLeft(2)} rather than {@code divide(...)}: division without a MathContext
     * throws on a non-terminating result, whereas a scale shift is exact and total. The empty guard
     * is required because {@code Converter.toBigDecimal} does not handle {@code ""}.
     *
     * <p>Known limitation: returns ZERO for unreadable text, conflating "no discount" with "could not
     * scrape the cell". Pre-existing behaviour, left as-is.
     */
    private BigDecimal parseDiscountRate(String text) {
        if(text == null || text.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var value = Converter.toBigDecimal(text);
        return value == null ? BigDecimal.ZERO : value.movePointLeft(2);
    }

    private Instant toInstant(String text) {
        return Converter.toInstant(text, TEXT_IMMEDIATE, TEXT_NEVER);
    }

    private Integer parseUsageLimit(String text) {
        return Converter.toInteger(text, TEXT_UNLIMITED);
    }
}
