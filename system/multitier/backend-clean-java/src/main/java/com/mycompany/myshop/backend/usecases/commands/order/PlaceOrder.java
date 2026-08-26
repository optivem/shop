package com.mycompany.myshop.backend.usecases.commands.order;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.services.YearEndBlackoutPolicy;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

import java.util.Optional;

public class PlaceOrder implements UseCase<PlaceOrderRequest, PlaceOrderResponse> {

    private static final String MSG_COUPON_DOES_NOT_EXIST = "Coupon code %s does not exist";

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final ErpGateway erpGateway;
    private final TaxGateway taxGateway;
    private final ClockGateway clockGateway;

    public PlaceOrder(OrderRepository orderRepository, CouponRepository couponRepository,
                      ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway) {
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.erpGateway = erpGateway;
        this.taxGateway = taxGateway;
        this.clockGateway = clockGateway;
    }

    // No try and no catch. Every step below can refuse, and each one says so by throwing; the
    // translation into a returned error happens once, in RefusalTranslatingUseCase, for this use
    // case and every other. What is left here is the business sequence and nothing else -- six
    // fallible steps read top to bottom, with no step unwrapping the one before it.
    //
    // This is the case that decides the rule. Written with a returned Result, the six steps become
    // six unwrap-and-return blocks around four values that all have to be in scope at once for the
    // single call to OrderPricing.price -- a fan-in, not a chain, which is the shape Java's lack of
    // a `?` operator punishes hardest.
    @Override
    public Result<PlaceOrderResponse, UseCaseError> execute(PlaceOrderRequest request) {
        var couponCode = CouponCode.ofNullable(request.getCouponCode());
        var orderTimestamp = clockGateway.getCurrentTime();

        YearEndBlackoutPolicy.requirePlacementAllowed(orderTimestamp);

        var sku = Sku.of(request.getSku());
        var unitPrice = unitPriceOf(sku);
        var promotionFactor = erpGateway.getPromotionDetails().factor();
        var coupon = findCoupon(couponCode);
        var discountRate = coupon
                .map(applicable -> applicable.discountAt(orderTimestamp))
                .orElse(Rate.ZERO);
        var country = Country.of(request.getCountry());
        var taxRate = taxRateOf(country);

        var pricing = OrderPricing.price(unitPrice, request.getQuantity(), promotionFactor,
                discountRate, taxRate);

        // The order records the code of the coupon that was applied, and there is no other case: a
        // Coupon cannot exist with a non-positive rate (its own constructor forbids it), so asking
        // whether the discount was positive was asking Coupon's invariant a second time, in a second
        // place. One owner, one expression.
        var appliedCouponCode = coupon.map(Coupon::getCode).orElse(null);
        var orderNumber = OrderNumber.generate();

        var order = Order.place(orderNumber, orderTimestamp, country, sku, pricing, appliedCouponCode);

        orderRepository.add(order);
        redeem(appliedCouponCode);

        return Result.ok(new PlaceOrderResponse(orderNumber.value()));
    }

    private void redeem(CouponCode appliedCouponCode) {
        if (appliedCouponCode == null) {
            return;
        }
        if (!couponRepository.tryRedeem(appliedCouponCode)) {
            // Rolls the order back with it: that is what the transaction boundary is for.
            throw Coupon.usageLimitReached(appliedCouponCode);
        }
    }

    private Optional<Coupon> findCoupon(Optional<CouponCode> requestedCode) {
        if (requestedCode.isEmpty()) {
            return Optional.empty();
        }
        var couponCode = requestedCode.get();

        var coupon = couponRepository.findByCode(couponCode);
        if (coupon.isEmpty()) {
            throw new ValidationException(CouponCode.FIELD_NAME,
                    String.format(MSG_COUPON_DOES_NOT_EXIST, couponCode));
        }

        return coupon;
    }

    private Money unitPriceOf(Sku sku) {
        var product = erpGateway.getProductDetails(sku);
        if (product.isEmpty()) {
            throw new ValidationException(Sku.FIELD_NAME, "Product does not exist for SKU: " + sku);
        }

        return product.get().price();
    }

    private Rate taxRateOf(Country country) {
        var taxRate = taxGateway.getTaxDetails(country);
        if (taxRate.isEmpty()) {
            throw new ValidationException("country", "Country does not exist: " + country);
        }

        return taxRate.get().rate();
    }

}
