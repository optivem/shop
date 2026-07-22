package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Every field is a {@code String} deliberately — do not "improve" these to typed fields.
 *
 * <p>Requests carry the raw text a user could type so negative tests can send values the domain must
 * reject: {@code PublishCouponNegativeTest} is parameterized over {@code String discountRate} and
 * sends {@code "abc"}, {@code "-0.5"}, {@code "1.5"} to assert the backend's validation messages. A
 * {@code BigDecimal} field makes those tests unwritable.
 *
 * <p>Assertions are the opposite: those are {@code BigDecimal}-canonical. String-on-the-request,
 * BigDecimal-on-the-assertion is the intended split, not an inconsistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishCouponRequest {
    private String code;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private String usageLimit;
}
