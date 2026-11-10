package com.mycompany.myshop.config;

import com.mycompany.myshop.controllers.web.storefront.StorefrontModelInterceptor;
import com.mycompany.myshop.core.services.ProductService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Gives every server-rendered page the storefront chrome's data (header, cart badge, categories). */
@Configuration
public class StorefrontWebConfig implements WebMvcConfigurer {

    private final ProductService productService;

    public StorefrontWebConfig(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new StorefrontModelInterceptor(productService))
                .excludePathPatterns("/api/**", "/health", "/actuator/**");
    }
}
