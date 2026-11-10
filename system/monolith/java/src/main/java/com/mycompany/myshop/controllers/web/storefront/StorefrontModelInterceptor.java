package com.mycompany.myshop.controllers.web.storefront;

import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.core.services.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Adds what the layout's header, footer and account sidebar need to every rendered page: the signed-in user,
 * the cart badge count, the catalog's categories and the current address (for "Sign in" to return to).
 * Redirects and API responses are left alone.
 */
public class StorefrontModelInterceptor implements HandlerInterceptor {

    /** A page that already loaded the catalog puts it here, so the header doesn't ask the ERP twice. */
    public static final String PRODUCTS_ATTRIBUTE = "products";

    private static final Logger log = LoggerFactory.getLogger(StorefrontModelInterceptor.class);

    private final ProductService productService;

    public StorefrontModelInterceptor(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null || isRedirect(modelAndView)) {
            return;
        }
        var model = modelAndView.getModel();
        model.put("storeUser", StoreUser.of(SecurityContextHolder.getContext().getAuthentication()));
        model.put("cartCount", Cart.peek(request).totalQuantity());
        model.putIfAbsent("categories", categories(model.get(PRODUCTS_ATTRIBUTE)));
        model.put("currentPath", request.getRequestURI());
        model.put("returnUrl", returnUrl(request));
        model.putIfAbsent("searchQuery", "");
        model.put("money", Money.Formatter.INSTANCE);
        model.put("brandColours", BrandColour.INSTANCE);
    }

    private static boolean isRedirect(ModelAndView modelAndView) {
        var viewName = modelAndView.getViewName();
        return viewName != null && viewName.startsWith("redirect:");
    }

    /** The header shows the catalog's categories on every page; an unavailable ERP leaves them out. */
    @SuppressWarnings("unchecked")
    private List<String> categories(Object loadedProducts) {
        if (loadedProducts instanceof List<?> products) {
            return CatalogQuery.categoriesOf((List<ViewProductDetailsResponse>) products);
        }
        try {
            var products = productService.browseProducts();
            return products == null ? List.of() : CatalogQuery.categoriesOf(products);
        } catch (RuntimeException e) {
            log.warn("Catalog unavailable for the header categories: {}", e.getMessage());
            return List.of();
        }
    }

    private static String returnUrl(HttpServletRequest request) {
        var query = request.getQueryString();
        return request.getRequestURI() + (query == null ? "" : "?" + query);
    }
}
