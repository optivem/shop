package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

/**
 * The customer checks out. The steps run in this order: put the cart lines in the cart (each from its product page,
 * in the order given); change the ERP; open the checkout (by its address, unless proceeding from the cart); enter the
 * shipping address and coupon; place the order when asked. SKUs and coupon codes are aliases; the shipping address
 * is literal, its country a code as the checkout's form offers it.
 */
public interface WhenCheckOut extends WhenStep {
    /** Before the checkout, the cart already contains this line. */
    WhenCheckOut withCartContaining(String skuAlias, int quantity);

    /** After the cart is filled, the ERP's price of this product changes. */
    WhenCheckOut withErpPriceChanged(String skuAlias, String unitPrice);

    /** After the cart is filled, the ERP no longer offers this product. */
    WhenCheckOut withErpNoLongerOffering(String skuAlias);

    /** Proceeds to checkout from the cart instead of opening the checkout by its address. */
    WhenCheckOut withProceededFromCart();

    /** Proceeds to checkout from the cart, and signs in as a customer when asked to. */
    WhenCheckOut withProceededFromCartAndSignedIn();

    /** Chooses to ship to this address instead of the saved one (the form is open anyway on a first order). */
    WhenCheckOut withShippingAddress(String firstName, String lastName, String street, String postalCode, String city,
                                     String country);

    /** Unticks "Save as my default shipping address", which a first order offers ticked. */
    WhenCheckOut withSaveAsDefaultUnticked();

    WhenCheckOut withCouponCode(String couponCode);

    /** Places the order; without this, the checkout is only opened (and the address entered). */
    WhenCheckOut withOrderPlaced();
}
