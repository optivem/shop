package com.mycompany.myshop.backend.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
    void browseProductsProxiesTheErpCatalog() {
        when(erpGateway.getProducts()).thenReturn(List.of(
                product("HP-15", "HP Laptop 15", "Laptops", "HP", "699.99"),
                product("LOGI-MX3S", "Logitech MX Master 3S", "Accessories", "Logitech", "99.99")));

        var response = productService.browseProducts();

        assertThat(response)
                .extracting("sku", "title", "description", "brand", "category", "price")
                .containsExactly(
                        Tuple.tuple("HP-15", "HP Laptop 15", "HP Laptop 15 description", "HP", "Laptops",
                                new BigDecimal("699.99")),
                        Tuple.tuple("LOGI-MX3S", "Logitech MX Master 3S", "Logitech MX Master 3S description",
                                "Logitech", "Accessories", new BigDecimal("99.99")));
    }

    @Test
    void browseProductsIsEmptyWhenTheErpHasNoProducts() {
        when(erpGateway.getProducts()).thenReturn(List.of());

        assertThat(productService.browseProducts()).isEmpty();
    }

    @Test
    void viewProductReturnsTheErpProduct() {
        when(erpGateway.getProductDetails("DELL-XPS"))
                .thenReturn(Optional.of(product("DELL-XPS", "Dell XPS 13", "Laptops", "Dell", "1299.99")));

        var response = productService.viewProduct("DELL-XPS");

        assertThat(response.getSku()).isEqualTo("DELL-XPS");
        assertThat(response.getTitle()).isEqualTo("Dell XPS 13");
        assertThat(response.getDescription()).isEqualTo("Dell XPS 13 description");
        assertThat(response.getBrand()).isEqualTo("Dell");
        assertThat(response.getCategory()).isEqualTo("Laptops");
        assertThat(response.getPrice()).isEqualByComparingTo("1299.99");
    }

    @Test
    void viewProductOfUnknownSkuDoesNotExist() {
        when(erpGateway.getProductDetails("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.viewProduct("UNKNOWN"))
                .isInstanceOf(NotExistValidationException.class)
                .hasMessage("Product UNKNOWN does not exist.");
    }

    private static ErpProductDetailsResponse product(String sku, String title, String category, String brand,
            String price) {
        var product = new ErpProductDetailsResponse();
        product.setId(sku);
        product.setTitle(title);
        product.setDescription(title + " description");
        product.setCategory(category);
        product.setBrand(brand);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}
