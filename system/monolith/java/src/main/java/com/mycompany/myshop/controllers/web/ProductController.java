package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.core.services.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** `/products/{sku}` (storefront-ux §4.2, UC-007): public; an unknown SKU is a 404 page. */
@Controller
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products/{sku}")
    public String product(@PathVariable String sku, Model model, HttpServletResponse response) {
        try {
            var product = productService.viewProduct(sku);
            if (product == null) {
                return notFound(response);
            }
            model.addAttribute("product", product);
        } catch (NotExistValidationException e) {
            return notFound(response);
        } catch (RuntimeException e) {
            log.warn("Product {} unavailable: {}", sku, e.getMessage());
            model.addAttribute("unavailableMessage", CatalogController.PRODUCTS_UNAVAILABLE);
        }
        return "product";
    }

    private static String notFound(HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return "product-not-found";
    }
}
