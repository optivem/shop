package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import org.springframework.stereotype.Service;

import java.util.List;

/** Products are owned by the ERP; MyShop only proxies its catalog. */
@Service
public class ProductService {

    private final ErpGateway erpGateway;

    public ProductService(ErpGateway erpGateway) {
        this.erpGateway = erpGateway;
    }

    public List<ViewProductDetailsResponse> browseProducts() {
        return erpGateway.getProducts().stream()
                .map(ProductService::toResponse)
                .toList();
    }

    public ViewProductDetailsResponse viewProduct(String sku) {
        return erpGateway.getProductDetails(sku)
                .map(ProductService::toResponse)
                .orElseThrow(() -> new NotExistValidationException("Product " + sku + " does not exist."));
    }

    private static ViewProductDetailsResponse toResponse(ErpProductDetailsResponse product) {
        var response = new ViewProductDetailsResponse();
        response.setSku(product.getId());
        response.setTitle(product.getTitle());
        response.setDescription(product.getDescription());
        response.setBrand(product.getBrand());
        response.setCategory(product.getCategory());
        response.setPrice(product.getPrice());
        return response;
    }
}
