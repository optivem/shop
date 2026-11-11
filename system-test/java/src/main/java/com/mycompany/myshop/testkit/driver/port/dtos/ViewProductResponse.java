package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The product page as shown. The API channel fills only the product fields; everything else is UI-only and stays
 * null there.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewProductResponse {
    private String sku;
    private String title;
    private String brand;
    private String category;
    private String description;
    /** The price as the channel shows it: "$1,299.99" on the UI, the plain amount on the API. */
    private String price;
    private List<String> breadcrumbs;
    /** The quantity field's value. */
    private String quantity;
    /** The notification, the "Product not found" heading or the ERP outage message, whichever shows. */
    private String message;
    private Boolean backToCatalogOffered;
    private Boolean addToCartOffered;
    /** The header's cart badge; 0 when it is not shown. */
    private Integer cartItemCount;
    /** The SKU of the product page the visitor is on, from the page address; null on any other page. */
    private String pageSku;
    /** The cart's lines, read only after adding to the cart. */
    private List<CartLineDto> cartLines;
    /** The catalog's products, read only after following the link back to the catalog. */
    private List<String> catalogSkus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartLineDto {
        private String sku;
        private Integer quantity;
    }
}
