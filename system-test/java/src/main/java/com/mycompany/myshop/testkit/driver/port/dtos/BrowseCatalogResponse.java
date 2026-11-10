package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The catalog as shown. The filter, sort and message fields are UI-only and stay null on the API channel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseCatalogResponse {
    private List<ProductDto> products;
    private Integer productCount;
    /** The category filter's options, "All" included. */
    private List<String> categories;
    private String selectedCategory;
    private List<String> brands;
    private List<String> tickedBrands;
    private String sort;
    private String message;
    private Boolean clearFiltersOffered;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {
        private String sku;
        private String title;
        private String brand;
        private String category;
        private String description;
        /** The price as the channel shows it: "$1,299.99" on the UI, the plain amount on the API. */
        private String price;
    }
}
