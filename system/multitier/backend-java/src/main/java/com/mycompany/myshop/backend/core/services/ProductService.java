package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductImage;
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

    // The photo is proxied rather than linked: the ERP is not reachable from the browser.
    public ErpProductImage getProductImage(String sku) {
        var imageUrl = erpGateway.getProductDetails(sku)
                .orElseThrow(() -> new NotExistValidationException("Product " + sku + " does not exist."))
                .getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new NotExistValidationException("Product " + sku + " has no image.");
        }
        return erpGateway.getProductImage(imageUrl)
                .orElseThrow(() -> new NotExistValidationException("Image of product " + sku + " does not exist."));
    }

    private static ViewProductDetailsResponse toResponse(ErpProductDetailsResponse product) {
        var response = new ViewProductDetailsResponse();
        response.setSku(product.getId());
        response.setTitle(product.getTitle());
        response.setDescription(product.getDescription());
        response.setBrand(product.getBrand());
        response.setCategory(product.getCategory());
        response.setPrice(product.getPrice());
        response.setImageUrl(imageUrlOf(product));
        return response;
    }

    private static String imageUrlOf(ErpProductDetailsResponse product) {
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            return null;
        }
        return "/api/products/" + product.getId() + "/image";
    }
}
