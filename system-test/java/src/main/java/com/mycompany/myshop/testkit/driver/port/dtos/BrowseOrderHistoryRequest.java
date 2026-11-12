package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseOrderHistoryRequest {
    /** {@link #followUp}: after browsing, set the customer filter to All customers and clear the order number (UI only). */
    public static final String RESET_FILTERS = "reset-filters";

    private String orderNumber;
    private String page;
    private String size;
    /** Admin filter: the customer's username; ignored for a customer. */
    private String customer;
    /** What the user does after browsing: {@link #RESET_FILTERS}, or nothing (unset). UI only. */
    private String followUp;
}
