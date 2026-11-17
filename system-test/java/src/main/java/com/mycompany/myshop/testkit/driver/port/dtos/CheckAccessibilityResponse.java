package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** The accessibility check of the page that is open. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckAccessibilityResponse {
    /** The serious and critical axe violations, one readable line each: rule, impact, help and the offending elements. */
    private List<String> violations;
}
