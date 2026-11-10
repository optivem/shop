package com.mycompany.myshop.controllers.web.storefront;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The visitor's cart (UC-008), kept in the HTTP session (storefront-ux §6.3). It holds only SKUs and
 * quantities, in the order the products were first added; prices are re-read from the catalog on display.
 * Spring Security keeps session attributes when it rotates the session ID at login, so the cart survives sign-in.
 */
public final class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    static final String SESSION_ATTRIBUTE = "myshop.cart";

    private final LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();

    /** The session's cart, created with the session when there is none yet. */
    public static Cart of(HttpSession session) {
        var cart = (Cart) session.getAttribute(SESSION_ATTRIBUTE);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(SESSION_ATTRIBUTE, cart);
        }
        return cart;
    }

    /** The request's cart without creating a session: an empty cart when there is no session yet. */
    public static Cart peek(HttpServletRequest request) {
        var session = request.getSession(false);
        var cart = session == null ? null : (Cart) session.getAttribute(SESSION_ATTRIBUTE);
        return cart == null ? new Cart() : cart;
    }

    /** Adds to a line, or starts one; a line never goes past the maximum quantity. */
    public void add(String sku, int quantity) {
        quantities.merge(sku, Quantity.clamp(quantity), (current, added) -> Quantity.clamp(current + added));
    }

    public void setQuantity(String sku, int quantity) {
        if (quantities.containsKey(sku)) {
            quantities.put(sku, Quantity.clamp(quantity));
        }
    }

    public void remove(String sku) {
        quantities.remove(sku);
    }

    public void removeAll(Collection<String> skus) {
        skus.forEach(quantities::remove);
    }

    public boolean isEmpty() {
        return quantities.isEmpty();
    }

    public int totalQuantity() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public List<CartLine> lines() {
        var lines = new ArrayList<CartLine>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            lines.add(new CartLine(entry.getKey(), entry.getValue()));
        }
        return lines;
    }

    public record CartLine(String sku, int quantity) implements Serializable {
    }
}
