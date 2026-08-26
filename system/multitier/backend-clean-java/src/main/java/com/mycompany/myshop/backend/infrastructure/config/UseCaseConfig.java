package com.mycompany.myshop.backend.infrastructure.config;

import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.usecases.decorators.LoggingUseCase;
import com.mycompany.myshop.backend.usecases.decorators.RefusalTranslatingUseCase;
import com.mycompany.myshop.backend.usecases.decorators.TransactionRunner;
import com.mycompany.myshop.backend.usecases.decorators.TransactionalUseCase;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.usecases.commands.coupon.PublishCoupon;
import com.mycompany.myshop.backend.usecases.commands.coupon.PublishCouponRequest;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryRequest;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.commands.order.CancelOrder;
import com.mycompany.myshop.backend.usecases.commands.order.CancelOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.DeliverOrder;
import com.mycompany.myshop.backend.usecases.commands.order.DeliverOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrder;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderResponse;
import com.mycompany.myshop.backend.usecases.commands.order.RecallSku;
import com.mycompany.myshop.backend.usecases.commands.order.RecallSkuRequest;
import com.mycompany.myshop.backend.usecases.commands.order.RecallSkuResponse;
import com.mycompany.myshop.backend.usecases.commands.order.SweepDeliveries;
import com.mycompany.myshop.backend.usecases.commands.order.SweepDeliveriesRequest;
import com.mycompany.myshop.backend.usecases.commands.order.SweepDeliveriesResponse;
import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetails;
import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsRequest;
import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.usecases.queries.coupon.ports.CouponReader;
import com.mycompany.myshop.backend.usecases.queries.order.ports.OrderReader;
import com.mycompany.myshop.backend.usecases.queries.report.ports.SalesReportReader;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReport;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportRequest;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportResponse;
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
    UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> viewOrderDetails(OrderReader orderReader) {
        return observed("ViewOrderDetails", new ViewOrderDetails(orderReader));
    }

    @Bean
    UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> browseOrderHistory(OrderReader orderReader) {
        return observed("BrowseOrderHistory", new BrowseOrderHistory(orderReader));
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
    UseCase<BrowseCouponsRequest, BrowseCouponsResponse> browseCoupons(CouponReader couponReader) {
        return observed("BrowseCoupons", new BrowseCoupons(couponReader));
    }

    @Bean
    UseCase<ViewSalesReportRequest, ViewSalesReportResponse> viewSalesReport(SalesReportReader salesReportReader) {
        return observed("ViewSalesReport", new ViewSalesReport(salesReportReader));
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
