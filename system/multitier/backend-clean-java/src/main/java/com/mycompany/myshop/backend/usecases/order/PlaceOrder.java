package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.policies.YearEndBlackoutPolicy;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Places an order: prices it, records it, and redeems the coupon it was placed with.
 *
 * <p>Orchestration only. When the order may be placed is {@code YearEndBlackoutPolicy}'s decision,
 * what it costs is {@code OrderPricing}'s, and whether the coupon may be used is the coupon's — this
 * class fetches, sequences and saves.
 */
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

    /**
     * The seam. Placement has six ways to be refused — blackout window, unknown SKU, unknown coupon,
     * coupon not yet valid, expired, or used up — and five of them are the domain's own judgement,
     * raised from inside {@code Coupon} and {@code YearEndBlackoutPolicy}. Translating once here
     * keeps {@link #place} in the order the pricing depends on; returning each refusal inline would
     * mean re-deriving that order, and the order is what decides which refusal a caller sees first.
     */
    @Override
    public Result<PlaceOrderResponse, UseCaseError> execute(PlaceOrderRequest request) {
        try {
            return Result.ok(place(request));
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }
    }

    private PlaceOrderResponse place(PlaceOrderRequest request) {
        var couponCode = CouponCode.requested(request.getCouponCode());
        var orderTimestamp = clockGateway.getCurrentTime();

        YearEndBlackoutPolicy.requirePlacementAllowed(orderTimestamp);

        var unitPrice = unitPriceOf(request.getSku());
        var promotionFactor = erpGateway.getPromotionDetails().factor();
        var coupon = findCoupon(couponCode);
        var discountRate = coupon
                .map(applicable -> applicable.discountAt(orderTimestamp))
                .orElse(Rate.ZERO);
        var country = Country.of(request.getCountry());
        var taxRate = taxRateOf(country);

        var pricing = OrderPricing.price(unitPrice, request.getQuantity(), promotionFactor,
                discountRate, taxRate);

        var appliedCouponCode = discountRate.isPositive() ? couponCode.orElse(null) : null;
        var orderNumber = generateOrderNumber();

        var order = new Order(orderNumber, orderTimestamp, country, request.getSku(),
                pricing, OrderStatus.PLACED, appliedCouponCode);

        orderRepository.save(order);

        if (appliedCouponCode != null) {
            coupon.ifPresent(redeemed -> {
                redeemed.redeem();
                couponRepository.save(redeemed);
            });
        }

        var response = new PlaceOrderResponse();
        response.setOrderNumber(orderNumber);
        return response;
    }

    /**
     * Empty when no coupon was asked for; a coupon that exists otherwise. Whether one was asked for
     * is {@link CouponCode#requested} 's decision, made before this is called.
     */
    private Optional<Coupon> findCoupon(Optional<CouponCode> couponCode) {
        if (couponCode.isEmpty()) {
            return Optional.empty();
        }

        var coupon = couponRepository.findByCode(couponCode.get());
        if (coupon.isEmpty()) {
            throw new ValidationException(CouponCode.FIELD_NAME,
                    String.format(MSG_COUPON_DOES_NOT_EXIST, couponCode.get()));
        }

        return coupon;
    }

    private Money unitPriceOf(String sku) {
        var product = erpGateway.getProductDetails(sku);
        if (product.isEmpty()) {
            throw new ValidationException("sku", "Product does not exist for SKU: " + sku);
        }

        return product.get().getPrice();
    }

    private Rate taxRateOf(Country country) {
        var taxRate = taxGateway.getTaxDetails(country);
        if (taxRate.isEmpty()) {
            throw new ValidationException("country", "Country does not exist: " + country);
        }

        return taxRate.get().getRate();
    }

    private String generateOrderNumber() {
        var uuid = UUID.randomUUID().toString().toUpperCase();
        return "ORD-" + uuid;
    }
}
