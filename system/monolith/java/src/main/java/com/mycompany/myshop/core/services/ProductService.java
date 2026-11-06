package com.mycompany.myshop.core.services;

import com.mycompany.myshop.core.dtos.ViewProductListResponse;
import com.mycompany.myshop.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.core.services.external.ErpGateway;
import org.springframework.stereotype.Service;

/** Products are owned by the ERP; MyShop only proxies its catalog. */
@Service
public class ProductService {

    private final ErpGateway erpGateway;

    public ProductService(ErpGateway erpGateway) {
        this.erpGateway = erpGateway;
    }

    public ViewProductListResponse viewProductList() {
        var response = new ViewProductListResponse();
        response.setProducts(erpGateway.getProducts().stream()
                .map(ProductService::toItem)
                .toList());
        return response;
    }

    private static ViewProductListResponse.ViewProductListItemResponse toItem(ErpProductDetailsResponse product) {
        var item = new ViewProductListResponse.ViewProductListItemResponse();
        item.setSku(product.getId());
        item.setPrice(product.getPrice());
        return item;
    }
}
