package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How the visitor browses the catalog, applied in this order: follow a header category link, search, pick a
 * category, tick brands, sort; then the follow-up, if any. Unset inputs are left untouched. Only the UI has these
 * controls: the API serves the unfiltered list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseCatalogRequest {
    /** Opens the same page address again. */
    public static final String REOPEN_PAGE_ADDRESS = "reopen-page-address";
    /** Clears the filters. */
    public static final String CLEAR_FILTERS = "clear-filters";
    /** Separates the ticked brands. */
    public static final String BRAND_SEPARATOR = ",";

    private String headerCategory;
    private String search;
    private String category;
    /** The brands to tick, joined with {@link #BRAND_SEPARATOR}. */
    private String brands;
    /** The sort order's label, e.g. "Price: low to high". */
    private String sort;
    /** {@link #REOPEN_PAGE_ADDRESS} or {@link #CLEAR_FILTERS}, run after the other inputs. */
    private String followUp;
}
