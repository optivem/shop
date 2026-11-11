package com.mycompany.myshop.testkit.dsl.port.then.steps.base;

import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCart;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCheckout;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCatalog;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenClock;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCountry;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCoupon;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenOrder;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenOrderHistory;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProduct;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProductPage;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProfile;

public interface ThenStep<T> {
    T and();

    ThenOrder order();

    ThenOrder order(String orderNumber);

    ThenOrderHistory orderHistory();

    ThenCoupon coupon();

    /** The profile the operation returned, or the acting user's profile as viewed now. */
    ThenProfile profile();

    ThenCoupon coupon(String couponCode);

    /** The catalog the executed browse showed. */
    ThenCatalog catalog();

    /** The product page the executed view showed. */
    ThenProductPage productPage();

    /** The cart as the visitor's action left it. */
    ThenCart cart();

    /** Where the checkout left the customer. */
    ThenCheckout checkout();

    /** The order the checkout placed for this product (an alias), as the system shows it now. */
    ThenOrder orderFor(String skuAlias);

    ThenClock clock();

    ThenProduct product(String skuAlias);

    ThenCountry country(String countryAlias);
}
