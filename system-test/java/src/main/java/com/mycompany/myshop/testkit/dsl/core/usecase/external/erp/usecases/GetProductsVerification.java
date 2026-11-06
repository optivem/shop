package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductsResponse;
import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts on the products a scenario staged, not on the list's size: the real ERP also lists
 * products seeded or created by other tests.
 */
public class GetProductsVerification extends ResponseVerification<GetProductsResponse> {
    public GetProductsVerification(GetProductsResponse response, UseCaseContext context) {
        super(response, context);
    }

    public GetProductsVerification containsProduct(String skuParamAlias, String expectedPrice) {
        var expectedSku = getContext().getParamValue(skuParamAlias);
        var product = findProduct(expectedSku);
        var actualPrice = product.getPrice();
        assertThat(actualPrice)
                .withFailMessage("Expected product '%s' to have price %s, but was %s", expectedSku, expectedPrice, actualPrice)
                .isEqualByComparingTo(Converter.toBigDecimal(expectedPrice));
        return this;
    }

    private GetProductResponse findProduct(String sku) {
        var products = getResponse().getProducts();
        return products.stream()
                .filter(product -> sku.equals(product.getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected product list to contain SKU '" + sku
                        + "', but it contained " + products.stream().map(GetProductResponse::getSku).toList()));
    }
}
