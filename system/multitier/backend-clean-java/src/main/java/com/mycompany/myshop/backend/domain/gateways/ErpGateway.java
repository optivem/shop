package com.mycompany.myshop.backend.domain.gateways;

import com.mycompany.myshop.backend.domain.entities.Product;
import com.mycompany.myshop.backend.domain.values.Promotion;

import java.util.Optional;

public interface ErpGateway {

    Optional<Product> getProductDetails(String sku);

    Promotion getPromotionDetails();
}
