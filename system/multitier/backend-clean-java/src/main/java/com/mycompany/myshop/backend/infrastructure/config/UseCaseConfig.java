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
import com.mycompany.myshop.backend.usecases.order.RecallSku;
import com.mycompany.myshop.backend.usecases.order.SweepDeliveries;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import com.mycompany.myshop.backend.usecases.TransactionRunner;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    PlaceOrder placeOrder(OrderRepository orderRepository, CouponRepository couponRepository,
                          ErpGateway erpGateway, TaxGateway taxGateway, ClockGateway clockGateway,
                          TransactionRunner transactionRunner) {
        return new PlaceOrder(orderRepository, couponRepository, erpGateway, taxGateway, clockGateway,
                transactionRunner);
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
    ViewOrderDetails viewOrderDetails(OrderQuery orderQuery) {
        return new ViewOrderDetails(orderQuery);
    }

    @Bean
    BrowseOrderHistory browseOrderHistory(OrderQuery orderQuery) {
        return new BrowseOrderHistory(orderQuery);
    }

    @Bean
    RecallSku recallSku(OrderRepository orderRepository) {
        return new RecallSku(orderRepository);
    }

    @Bean
    SweepDeliveries sweepDeliveries(OrderRepository orderRepository, ClockGateway clockGateway) {
        return new SweepDeliveries(orderRepository, clockGateway);
    }

    @Bean
    PublishCoupon publishCoupon(CouponRepository couponRepository) {
        return new PublishCoupon(couponRepository);
    }

    @Bean
    BrowseCoupons browseCoupons(CouponQuery couponQuery) {
        return new BrowseCoupons(couponQuery);
    }
}
