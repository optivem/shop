package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.controllers.web.storefront.Cart;
import com.mycompany.myshop.controllers.web.storefront.Notice;
import com.mycompany.myshop.controllers.web.storefront.Quantity;
import com.mycompany.myshop.controllers.web.storefront.StorefrontModelInterceptor;
import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.core.services.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * `/cart` (storefront-ux §4.3, UC-008): the session cart at current catalog prices. Changes are plain form posts
 * that redirect back (PRG), so the cart works without JavaScript (storefront-ux §6.3).
 */
@Controller
public class CartController {

    static final String ADDED_TO_CART = "Added to cart";
    private static final String REDIRECT_CART = "redirect:/cart";

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final ProductService productService;

    public CartController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/cart")
    public String cart(HttpServletRequest request, Model model) {
        var cart = Cart.peek(request);
        var unavailable = false;
        List<ViewProductDetailsResponse> products = List.of();
        if (!cart.isEmpty()) {
            // Prices are re-read from the catalog whenever the cart is shown (UC-008 BR-5).
            try {
                var loaded = productService.browseProducts();
                products = loaded == null ? List.of() : loaded;
                model.addAttribute(StorefrontModelInterceptor.PRODUCTS_ATTRIBUTE, products);
            } catch (RuntimeException e) {
                log.warn("Catalog unavailable for the cart: {}", e.getMessage());
                unavailable = true;
            }
        }
        var bySku = products.stream().collect(Collectors.toMap(ViewProductDetailsResponse::getSku, Function.identity(), (a, b) -> a));
        var lines = cart.lines().stream().map(line -> PricedLine.of(line, bySku)).toList();
        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", lines.stream().map(PricedLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("unavailable", unavailable);
        model.addAttribute("unavailableMessage", CatalogController.PRODUCTS_UNAVAILABLE);
        return "cart";
    }

    /** "Add to cart" from the catalog (quantity 1) or the product page; goes back to where it was clicked. */
    @PostMapping("/cart/add")
    public String add(@RequestParam(required = false) String sku,
                      @RequestParam(required = false, defaultValue = "1") String quantity,
                      @RequestParam(required = false) String returnUrl,
                      HttpSession session, RedirectAttributes redirectAttributes) {
        var parsed = Quantity.parse(quantity);
        if (sku == null || sku.isBlank()) {
            redirectAttributes.addFlashAttribute(Notice.ATTRIBUTE, Notice.error("SKU must not be empty"));
        } else if (parsed.isEmpty()) {
            redirectAttributes.addFlashAttribute(Notice.ATTRIBUTE, Notice.error(Quantity.ERROR));
        } else {
            Cart.of(session).add(sku, parsed.get());
            redirectAttributes.addFlashAttribute(Notice.ATTRIBUTE, Notice.success(ADDED_TO_CART));
        }
        return "redirect:" + SignInController.safeReturnUrl(returnUrl);
    }

    /** A typed quantity is checked when committed; one that is not 1..99 keeps the previous quantity. */
    @PostMapping("/cart/update")
    public String update(@RequestParam(required = false) String sku,
                         @RequestParam(required = false) String quantity,
                         HttpSession session, RedirectAttributes redirectAttributes) {
        var parsed = Quantity.parse(quantity);
        if (parsed.isEmpty()) {
            redirectAttributes.addFlashAttribute(Notice.ATTRIBUTE, Notice.error(Quantity.ERROR));
        } else if (sku != null) {
            Cart.of(session).setQuantity(sku, parsed.get());
        }
        return REDIRECT_CART;
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam(required = false) String sku, HttpSession session) {
        if (sku != null) {
            Cart.of(session).remove(sku);
        }
        return REDIRECT_CART;
    }

    /**
     * One cart line at the current catalog price; {@code product} is null when the catalog no longer offers the SKU.
     */
    public record PricedLine(String sku, int quantity, ViewProductDetailsResponse product, BigDecimal lineTotal) {

        static PricedLine of(Cart.CartLine line, Map<String, ViewProductDetailsResponse> bySku) {
            var product = bySku.get(line.sku());
            var lineTotal = product == null || product.getPrice() == null ? BigDecimal.ZERO
                    : product.getPrice().multiply(BigDecimal.valueOf(line.quantity()));
            return new PricedLine(line.sku(), line.quantity(), product, lineTotal);
        }

        public int decreased() {
            return Quantity.clamp(quantity - 1);
        }

        public int increased() {
            return Quantity.clamp(quantity + 1);
        }
    }
}
