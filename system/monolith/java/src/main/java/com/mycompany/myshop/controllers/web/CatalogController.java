package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.controllers.web.storefront.CatalogQuery;
import com.mycompany.myshop.controllers.web.storefront.StorefrontModelInterceptor;
import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.core.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/** `/` (storefront-ux §4.1, UC-006): the catalog, public. The query string holds the filters. */
@Controller
public class CatalogController {

    static final String PRODUCTS_UNAVAILABLE = "Products are temporarily unavailable.";

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    private final ProductService productService;

    public CatalogController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String catalog(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String category,
                          @RequestParam(name = "brand", required = false) List<String> brands,
                          @RequestParam(required = false) String sort,
                          @RequestParam Map<String, String> params,
                          Model model) {
        var query = CatalogQuery.parse(q, category, brands, sort);
        // The filter form sends every field, defaults included; redirect to the short address of the same view.
        if (hasDefaultParams(params)) {
            return "redirect:" + query.path();
        }

        List<ViewProductDetailsResponse> products;
        var unavailable = false;
        try {
            var loaded = productService.browseProducts();
            products = loaded == null ? List.of() : loaded;
        } catch (RuntimeException e) {
            log.warn("Catalog unavailable: {}", e.getMessage());
            products = List.of();
            unavailable = true;
        }

        model.addAttribute(StorefrontModelInterceptor.PRODUCTS_ATTRIBUTE, products);
        model.addAttribute("shown", query.apply(products));
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("unavailableMessage", PRODUCTS_UNAVAILABLE);
        model.addAttribute("query", query);
        model.addAttribute("searchQuery", query.q());
        model.addAttribute("filterCategories", CatalogQuery.categoriesOf(products));
        model.addAttribute("filterBrands", CatalogQuery.brandsOf(products));
        model.addAttribute("sortOptions", CatalogQuery.SORT_OPTIONS);
        return "catalog";
    }

    private static boolean hasDefaultParams(Map<String, String> params) {
        return params.values().stream().anyMatch(String::isEmpty) || CatalogQuery.FEATURED.equals(params.get("sort"));
    }
}
