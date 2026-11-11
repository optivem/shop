package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ProductController {
    private static final String ENDPOINT = "/api/products";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public ProductController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<List<BrowseCatalogResponse.ProductDto>, ProblemDetailResponse> browseProducts() {
        return httpClient.get(ENDPOINT, BrowseCatalogResponse.ProductDto[].class)
                .map(Arrays::asList);
    }

    public Result<BrowseCatalogResponse.ProductDto, ProblemDetailResponse> viewProduct(String sku) {
        var encodedSku = URLEncoder.encode(sku, StandardCharsets.UTF_8).replace("+", "%20");
        return httpClient.get(ENDPOINT + "/" + encodedSku, BrowseCatalogResponse.ProductDto.class);
    }
}
