package com.mycompany.myshop.api.controller;

import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.core.services.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The catalog is public: both routes are reachable without signing in (see SecurityConfig). */
@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ViewProductDetailsResponse> browseProducts() {
        return productService.browseProducts();
    }

    @GetMapping("/{sku}")
    public ViewProductDetailsResponse viewProduct(@PathVariable String sku) {
        return productService.viewProduct(sku);
    }
}
