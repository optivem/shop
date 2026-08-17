package com.mycompany.myshop.backend.infrastructure.config;

import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.coupon.PublishCoupon;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.order.CancelOrder;
import com.mycompany.myshop.backend.usecases.order.DeliverOrder;
import com.mycompany.myshop.backend.usecases.order.PlaceOrder;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the use cases. They are plain classes with constructors — Spring finds them here rather than
 * by scanning for a {@code @Service} annotation on them, which is what keeps the container out of
 * the inside: the use case layer does not know it is being wired, and its tests construct it with
 * {@code new}.
 *
 * <p>The cost is this file. The benefit is that the dependency arrow points the right way, and
 * {@code ArchitectureTest} can say so unconditionally.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    PlaceOrder placeOrder(OrderRepository orderRepository, CouponRepository couponRepository,
                          ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway) {
        return new PlaceOrder(orderRepository, couponRepository, erpGateway, taxGateway, clockGateway);
    }

    @Bean
    CancelOrder cancelOrder(OrderRepository orderRepository, ClockGateway clockGateway) {
        return new CancelOrder(orderRepository, clockGateway);
    }

    @Bean
    DeliverOrder deliverOrder(OrderRepository orderRepository) {
        return new DeliverOrder(orderRepository);
    }

    @Bean
    ViewOrderDetails viewOrderDetails(OrderRepository orderRepository) {
        return new ViewOrderDetails(orderRepository);
    }

    @Bean
    BrowseOrderHistory browseOrderHistory(OrderRepository orderRepository) {
        return new BrowseOrderHistory(orderRepository);
    }

    @Bean
    PublishCoupon publishCoupon(CouponRepository couponRepository) {
        return new PublishCoupon(couponRepository);
    }

    @Bean
    BrowseCoupons browseCoupons(CouponRepository couponRepository) {
        return new BrowseCoupons(couponRepository);
    }
}
