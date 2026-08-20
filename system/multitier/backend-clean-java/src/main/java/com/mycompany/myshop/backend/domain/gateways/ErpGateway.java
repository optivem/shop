package com.mycompany.myshop.backend.domain.gateways;

import com.mycompany.myshop.backend.domain.entities.Product;
import com.mycompany.myshop.backend.domain.values.Promotion;
import com.mycompany.myshop.backend.domain.values.Sku;

import java.util.Optional;

public interface ErpGateway {

    Optional<Product> getProductDetails(Sku sku);

    Promotion getPromotionDetails();
}
