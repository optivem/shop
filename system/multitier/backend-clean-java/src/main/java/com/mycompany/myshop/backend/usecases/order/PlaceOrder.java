package com.mycompany.myshop.backend.usecases.order;

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
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.TransactionRunner;
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
    private final TransactionRunner transactionRunner;

    public PlaceOrder(OrderRepository orderRepository, CouponRepository couponRepository,
                      ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway,
                      TransactionRunner transactionRunner) {
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.erpGateway = erpGateway;
        this.taxGateway = taxGateway;
        this.clockGateway = clockGateway;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<PlaceOrderResponse, UseCaseError> execute(PlaceOrderRequest request) {
        try {
            return Result.ok(transactionRunner.inTransaction(() -> place(request)));
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }
    }

    private PlaceOrderResponse place(PlaceOrderRequest request) {
        var couponCode = CouponCode.requested(request.getCouponCode());
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

        var appliedCouponCode = discountRate.isPositive() ? couponCode.orElse(null) : null;
        var orderNumber = OrderNumber.generate();

        var order = new Order(orderNumber, orderTimestamp, country, sku,
                pricing, OrderStatus.PLACED, appliedCouponCode);

        orderRepository.save(order);
        redeem(appliedCouponCode);

        var response = new PlaceOrderResponse();
        response.setOrderNumber(orderNumber.value());
        return response;
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

    private Money unitPriceOf(Sku sku) {
        var product = erpGateway.getProductDetails(sku);
        if (product.isEmpty()) {
            throw new ValidationException(Sku.FIELD_NAME, "Product does not exist for SKU: " + sku);
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

}
