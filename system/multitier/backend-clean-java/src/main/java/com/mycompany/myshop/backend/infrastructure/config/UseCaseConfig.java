package com.mycompany.myshop.backend.infrastructure.config;

import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.LoggingUseCase;
import com.mycompany.myshop.backend.usecases.RefusalTranslatingUseCase;
import com.mycompany.myshop.backend.usecases.TransactionRunner;
import com.mycompany.myshop.backend.usecases.TransactionalUseCase;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.usecases.coupon.PublishCoupon;
import com.mycompany.myshop.backend.usecases.coupon.PublishCouponRequest;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryRequest;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.order.CancelOrder;
import com.mycompany.myshop.backend.usecases.order.CancelOrderRequest;
import com.mycompany.myshop.backend.usecases.order.DeliverOrder;
import com.mycompany.myshop.backend.usecases.order.DeliverOrderRequest;
import com.mycompany.myshop.backend.usecases.order.PlaceOrder;
import com.mycompany.myshop.backend.usecases.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.order.PlaceOrderResponse;
import com.mycompany.myshop.backend.usecases.order.RecallSku;
import com.mycompany.myshop.backend.usecases.order.RecallSkuRequest;
import com.mycompany.myshop.backend.usecases.order.RecallSkuResponse;
import com.mycompany.myshop.backend.usecases.order.SweepDeliveries;
import com.mycompany.myshop.backend.usecases.order.SweepDeliveriesRequest;
import com.mycompany.myshop.backend.usecases.order.SweepDeliveriesResponse;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetailsRequest;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;
import com.mycompany.myshop.backend.usecases.queries.OrderQuery;
import com.mycompany.myshop.backend.usecases.queries.SalesReportQuery;
import com.mycompany.myshop.backend.usecases.report.ViewSalesReport;
import com.mycompany.myshop.backend.usecases.report.ViewSalesReportRequest;
import com.mycompany.myshop.backend.usecases.report.ViewSalesReportResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Composition, written out rather than conjured by a convention. Every use case is wrapped in the
// same observed() pipeline, and the one that needs it is additionally wrapped in transacted() -- so
// which use cases run in a transaction is a list you can read in one file instead of a hunt through
// annotations. No proxies and no mediator: the controller already knows which use case it wants, so
// dispatch-by-request-type would buy nothing and hide the wiring.
@Configuration
public class UseCaseConfig {

    @Bean
    UseCase<PlaceOrderRequest, PlaceOrderResponse> placeOrder(
            OrderRepository orderRepository, CouponRepository couponRepository, ErpGateway erpGateway,
            TaxGateway taxGateway, ClockGateway clockGateway, TransactionRunner transactionRunner) {
        return transacted("PlaceOrder", transactionRunner, new PlaceOrder(orderRepository,
                couponRepository, erpGateway, taxGateway, clockGateway));
    }

    @Bean
    UseCase<CancelOrderRequest, Void> cancelOrder(OrderRepository orderRepository,
                                                  ClockGateway clockGateway) {
        return observed("CancelOrder", new CancelOrder(orderRepository, clockGateway));
    }

    @Bean
    UseCase<DeliverOrderRequest, Void> deliverOrder(OrderRepository orderRepository) {
        return observed("DeliverOrder", new DeliverOrder(orderRepository));
    }

    @Bean
    UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> viewOrderDetails(OrderQuery orderQuery) {
        return observed("ViewOrderDetails", new ViewOrderDetails(orderQuery));
    }

    @Bean
    UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> browseOrderHistory(OrderQuery orderQuery) {
        return observed("BrowseOrderHistory", new BrowseOrderHistory(orderQuery));
    }

    @Bean
    UseCase<RecallSkuRequest, RecallSkuResponse> recallSku(OrderRepository orderRepository) {
        return observed("RecallSku", new RecallSku(orderRepository));
    }

    @Bean
    UseCase<SweepDeliveriesRequest, SweepDeliveriesResponse> sweepDeliveries(
            OrderRepository orderRepository, ClockGateway clockGateway) {
        return observed("SweepDeliveries", new SweepDeliveries(orderRepository, clockGateway));
    }

    @Bean
    UseCase<PublishCouponRequest, Void> publishCoupon(CouponRepository couponRepository) {
        return observed("PublishCoupon", new PublishCoupon(couponRepository));
    }

    @Bean
    UseCase<BrowseCouponsRequest, BrowseCouponsResponse> browseCoupons(CouponQuery couponQuery) {
        return observed("BrowseCoupons", new BrowseCoupons(couponQuery));
    }

    @Bean
    UseCase<ViewSalesReportRequest, ViewSalesReportResponse> viewSalesReport(SalesReportQuery salesReportQuery) {
        return observed("ViewSalesReport", new ViewSalesReport(salesReportQuery));
    }

    // Every use case gets the refusal translator, which is the point of putting it here: the
    // guarantee "a refusal always reaches the caller as an error" is made once, by construction,
    // instead of being re-made by each use case remembering to catch.
    //
    // The order is not arbitrary. Translating sits INSIDE logging so the log sees one uniform thing
    // -- a Result -- for every outcome a caller can be given, and OUTSIDE the transaction so a
    // thrown refusal passes the transaction boundary, and rolls it back, while it is still an
    // exception. Translate first and the rollback would have nothing to see.
    private static <Q, S> UseCase<Q, S> observed(String name, UseCase<Q, S> useCase) {
        return new LoggingUseCase<>(name, new RefusalTranslatingUseCase<>(useCase));
    }

    // Only PlaceOrder needs one today: it is the use case that writes two aggregates, so it is the
    // one that can leave an order behind if the coupon it applied turns out not to be redeemable.
    // The other writers each issue a single statement, which the database already makes atomic.
    private static <Q, S> UseCase<Q, S> transacted(String name, TransactionRunner transactionRunner,
                                                   UseCase<Q, S> useCase) {
        return observed(name, new TransactionalUseCase<>(transactionRunner, useCase));
    }
}
