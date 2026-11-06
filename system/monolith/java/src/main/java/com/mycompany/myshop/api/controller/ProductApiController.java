package com.mycompany.myshop.api.controller;

import com.mycompany.myshop.core.dtos.ViewProductListResponse;
import com.mycompany.myshop.core.services.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ViewProductListResponse viewProductList() {
        return productService.viewProductList();
    }
}
