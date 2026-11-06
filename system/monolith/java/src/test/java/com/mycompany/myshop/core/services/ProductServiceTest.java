package com.mycompany.myshop.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.core.services.external.ErpGateway;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ErpGateway erpGateway;

    @InjectMocks
    private ProductService productService;

    @Test
    void viewProductListProxiesTheErpCatalog() {
        when(erpGateway.getProducts()).thenReturn(List.of(product("HP-15", "699.99"), product("DELL-XPS", "1299.99")));

        var response = productService.viewProductList();

        assertThat(response.getProducts())
                .extracting("sku", "price")
                .containsExactly(
                        Tuple.tuple("HP-15", new BigDecimal("699.99")),
                        Tuple.tuple("DELL-XPS", new BigDecimal("1299.99")));
    }

    @Test
    void viewProductListIsEmptyWhenTheErpHasNoProducts() {
        when(erpGateway.getProducts()).thenReturn(List.of());

        var response = productService.viewProductList();

        assertThat(response.getProducts()).isEmpty();
    }

    private static ErpProductDetailsResponse product(String sku, String price) {
        var product = new ErpProductDetailsResponse();
        product.setId(sku);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}
