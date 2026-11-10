package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.controllers.web.storefront.Cart;
import com.mycompany.myshop.controllers.web.storefront.CheckoutPlacer;
import com.mycompany.myshop.controllers.web.storefront.CheckoutResult;
import com.mycompany.myshop.controllers.web.storefront.Notice;
import com.mycompany.myshop.controllers.web.storefront.ShippingAddresses;
import com.mycompany.myshop.controllers.web.storefront.StorefrontModelInterceptor;
import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.ShippingAddress;
import com.mycompany.myshop.core.dtos.UpdateProfileRequest;
import com.mycompany.myshop.core.dtos.ViewProductDetailsResponse;
import com.mycompany.myshop.core.dtos.ViewProfileResponse;
import com.mycompany.myshop.core.services.OrderService;
import com.mycompany.myshop.core.services.ProductService;
import com.mycompany.myshop.core.services.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * `/checkout` and `/checkout/confirmation` (customers only, storefront-ux §4.4–§4.5, UC-009): shipping address,
 * optional coupon and the order review; places one order per cart line.
 */
@Controller
public class CheckoutController {

    private static final String SAVED = "saved";
    private static final String OTHER = "other";
    private static final String REDIRECT_CART = "redirect:/cart";
    private static final String CHECKOUT_VIEW = "checkout";

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final ProductService productService;
    private final ProfileService profileService;
    private final CheckoutPlacer placer;

    public CheckoutController(ProductService productService, ProfileService profileService, OrderService orderService,
                              Validator validator) {
        this.productService = productService;
        this.profileService = profileService;
        this.placer = new CheckoutPlacer(orderService, validator);
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Authentication authentication, Model model) {
        var cart = Cart.of(session);
        // An empty cart has nothing to check out (UC-009 3a).
        if (cart.isEmpty()) {
            return REDIRECT_CART;
        }
        var profile = loadProfile(CurrentUserResolver.resolve(authentication));
        var saved = ShippingAddresses.savedAddressOf(profile);
        // "Ship to a different address" starts from the customer's own names, with the address left to fill in.
        var address = ShippingAddresses.empty();
        if (profile != null) {
            address.setFirstName(orEmpty(saved != null ? saved.getFirstName() : profile.getFirstName()));
            address.setLastName(orEmpty(saved != null ? saved.getLastName() : profile.getLastName()));
        }
        return render(model, cart, saved, saved != null ? SAVED : OTHER, address, Map.of(), true, "");
    }

    @PostMapping("/checkout")
    public String placeCheckoutOrder(@RequestParam(required = false) String addressChoice,
                                     @RequestParam(required = false, defaultValue = "") String firstName,
                                     @RequestParam(required = false, defaultValue = "") String lastName,
                                     @RequestParam(required = false, defaultValue = "") String street,
                                     @RequestParam(required = false, defaultValue = "") String postalCode,
                                     @RequestParam(required = false, defaultValue = "") String city,
                                     @RequestParam(required = false, defaultValue = "") String country,
                                     @RequestParam(required = false) String saveAsDefault,
                                     @RequestParam(required = false, defaultValue = "") String couponCode,
                                     HttpSession session, Authentication authentication, Model model) {
        var cart = Cart.of(session);
        if (cart.isEmpty()) {
            return REDIRECT_CART;
        }
        var user = CurrentUserResolver.resolve(authentication);
        var saved = ShippingAddresses.savedAddressOf(loadProfile(user));
        var choice = saved != null && SAVED.equals(addressChoice) ? SAVED : OTHER;
        var entered = new ShippingAddress(firstName, lastName, street, postalCode, city, country);
        var saveChecked = saveAsDefault != null;

        var fieldErrors = SAVED.equals(choice) ? Map.<String, String>of() : ShippingAddresses.validate(entered);
        if (!fieldErrors.isEmpty()) {
            model.addAttribute(Notice.ATTRIBUTE,
                    Notice.error("Please correct the shipping address.", List.copyOf(fieldErrors.values())));
            return render(model, cart, saved, choice, entered, fieldErrors, saveChecked, couponCode);
        }

        var target = SAVED.equals(choice) ? saved : entered.trimmed();
        var code = couponCode.isBlank() ? null : couponCode.trim();
        var result = placer.place(cart.lines(), target, code, user);

        if (result.placed().isEmpty()) {
            // UC-009 8b: nothing was placed; the cart stays as it is.
            var reasons = result.rejected().stream().map(line -> line.sku() + ": " + line.reason()).toList();
            model.addAttribute(Notice.ATTRIBUTE, Notice.error("No order was placed.", reasons));
            return render(model, cart, saved, choice, entered, Map.of(), saveChecked, couponCode);
        }

        if (saved == null && saveChecked) {
            saveAsDefaultAddress(target, user);
        }
        // UC-009 8a: the placed lines leave the cart and the rejected ones stay in it.
        cart.removeAll(result.placedSkus());
        session.setAttribute(CheckoutResult.SESSION_ATTRIBUTE, result);
        return "redirect:/checkout/confirmation";
    }

    /** The orders just placed (and, after a partial failure, the rejected lines). */
    @GetMapping("/checkout/confirmation")
    public String confirmation(HttpSession session, Model model) {
        model.addAttribute("result", session.getAttribute(CheckoutResult.SESSION_ATTRIBUTE));
        return "checkout-confirmation";
    }

    private String render(Model model, Cart cart, ShippingAddress saved, String choice, ShippingAddress address,
                          Map<String, String> fieldErrors, boolean saveAsDefault, String couponCode) {
        var products = loadProducts();
        model.addAttribute(StorefrontModelInterceptor.PRODUCTS_ATTRIBUTE, products);
        var bySku = products.stream().collect(Collectors.toMap(ViewProductDetailsResponse::getSku, Function.identity(), (a, b) -> a));
        var lines = cart.lines().stream().map(line -> CartController.PricedLine.of(line, bySku)).toList();
        var shippingCountry = SAVED.equals(choice) && saved != null ? saved.getCountry() : address.getCountry();

        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", lines.stream().map(CartController.PricedLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("saved", saved);
        model.addAttribute("savedText", saved == null ? null : ShippingAddresses.format(saved));
        model.addAttribute("choice", choice);
        model.addAttribute("address", address);
        model.addAttribute("fieldErrors", fieldErrors);
        model.addAttribute("firstOrder", saved == null);
        model.addAttribute("saveAsDefault", saveAsDefault);
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("countries", ShippingAddresses.COUNTRIES);
        model.addAttribute("taxCountry", shippingCountry == null || shippingCountry.isBlank() ? "-" : shippingCountry.trim());
        model.addAttribute("taxRate", ShippingAddresses.estimatedTaxRate(shippingCountry));
        model.addAttribute("taxRates", ShippingAddresses.ESTIMATED_TAX_RATES.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), ShippingAddresses::estimatedTaxRate)));
        return CHECKOUT_VIEW;
    }

    /** A profile that can't be read is treated like an empty one: checkout never blocks on it. */
    private ViewProfileResponse loadProfile(CurrentUser user) {
        try {
            return profileService.getProfile(user);
        } catch (RuntimeException e) {
            log.warn("Profile unavailable at checkout: {}", e.getMessage());
            return null;
        }
    }

    private List<ViewProductDetailsResponse> loadProducts() {
        try {
            var products = productService.browseProducts();
            return products == null ? List.of() : products;
        } catch (RuntimeException e) {
            log.warn("Catalog unavailable at checkout: {}", e.getMessage());
            return List.of();
        }
    }

    /** Best effort: the orders are placed either way. */
    private void saveAsDefaultAddress(ShippingAddress address, CurrentUser user) {
        var request = new UpdateProfileRequest();
        request.setFirstName(address.getFirstName());
        request.setLastName(address.getLastName());
        request.setStreet(address.getStreet());
        request.setPostalCode(address.getPostalCode());
        request.setCity(address.getCity());
        request.setCountry(address.getCountry());
        try {
            profileService.updateProfile(request, user);
        } catch (RuntimeException e) {
            log.warn("Could not save the checkout address to the profile: {}", e.getMessage());
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
