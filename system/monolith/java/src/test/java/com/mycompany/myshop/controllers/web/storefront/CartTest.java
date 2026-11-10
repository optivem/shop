package com.mycompany.myshop.controllers.web.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CartTest {

    @Test
    void addingAProductAlreadyInTheCartRaisesItsQuantity() {
        var cart = new Cart();
        cart.add("HP-15", 1);
        cart.add("HP-15", 2);

        assertThat(cart.lines()).containsExactly(new Cart.CartLine("HP-15", 3));
    }

    @Test
    void linesKeepTheOrderTheirProductsWereFirstAdded() {
        var cart = new Cart();
        cart.add("LG-27UL500", 1);
        cart.add("HP-15", 1);
        cart.add("LG-27UL500", 1);

        assertThat(cart.lines()).extracting(Cart.CartLine::sku).containsExactly("LG-27UL500", "HP-15");
    }

    @Test
    void aLineIsCappedAt99() {
        var cart = new Cart();
        cart.add("HP-15", 98);
        cart.add("HP-15", 5);

        assertThat(cart.totalQuantity()).isEqualTo(99);
    }

    @Test
    void settingTheQuantityOfAnUnknownSkuAddsNothing() {
        var cart = new Cart();
        cart.setQuantity("HP-15", 3);

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void removingPlacedLinesKeepsTheRest() {
        var cart = new Cart();
        cart.add("HP-15", 1);
        cart.add("DELL-XPS", 2);
        cart.removeAll(List.of("HP-15"));

        assertThat(cart.lines()).containsExactly(new Cart.CartLine("DELL-XPS", 2));
        assertThat(cart.totalQuantity()).isEqualTo(2);
    }
}
