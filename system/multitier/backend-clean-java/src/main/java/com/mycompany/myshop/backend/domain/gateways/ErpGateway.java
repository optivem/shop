package com.mycompany.myshop.backend.domain.gateways;

import com.mycompany.myshop.backend.domain.entities.Product;
import com.mycompany.myshop.backend.domain.entities.Promotion;

import java.util.Optional;

/**
 * The domain's port to the ERP. Returns domain types only — the ERP's JSON shape is the adapter's
 * problem, not the centre's.
 */
public interface ErpGateway {

    /**
     * @return the product, or empty when the ERP does not know the SKU.
     */
    Optional<Product> getProductDetails(String sku);

    Promotion getPromotionDetails();
}
