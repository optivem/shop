import type { Browser } from '@playwright/test';
import type { Result } from '../../../common/result.js';
import { success, failure } from '../../../common/result.js';
import type { GoToMyShopRequest } from '../../port/dtos/GoToMyShopRequest.js';
import type { GoToMyShopResponse } from '../../port/dtos/GoToMyShopResponse.js';
import type { PlaceOrderRequest } from '../../port/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../port/dtos/PlaceOrderResponse.js';
import type { CancelOrderRequest } from '../../port/dtos/CancelOrderRequest.js';
import type { CancelOrderResponse } from '../../port/dtos/CancelOrderResponse.js';
import type { DeliverOrderRequest } from '../../port/dtos/DeliverOrderRequest.js';
import type { DeliverOrderResponse } from '../../port/dtos/DeliverOrderResponse.js';
import type { ViewOrderRequest } from '../../port/dtos/ViewOrderRequest.js';
import type { ViewOrderResponse } from '../../port/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../port/dtos/errors/SystemError.js';
import type { PublishCouponRequest } from '../../port/dtos/PublishCouponRequest.js';
import type { PublishCouponResponse } from '../../port/dtos/PublishCouponResponse.js';
import type { BrowseCouponsRequest } from '../../port/dtos/BrowseCouponsRequest.js';
import type { BrowseCouponsResponse } from '../../port/dtos/BrowseCouponsResponse.js';
import type { MyShopDriver } from '../../port/my-shop-driver.js';
import { MyShopUiClient } from './client/MyShopUiClient.js';
import { NewOrderPage } from './client/pages/NewOrderPage.js';

export class MyShopUiDriver implements MyShopDriver {
  private readonly client: MyShopUiClient;

  constructor(baseUrl: string, browser: Browser) {
    this.client = new MyShopUiClient(baseUrl, browser);
  }

  async goToMyShop(_request: GoToMyShopRequest): Promise<Result<GoToMyShopResponse, SystemError>> {
    const result = await this.client.openHomePage();
    if (result.success) return success({});
    return failure(result.error);
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickNewOrder();

    const newOrderPage = this.client.newOrderPage();
    if (request.sku !== null) {
      await newOrderPage.inputSku(request.sku);
    }
    if (request.quantity !== null) {
      await newOrderPage.inputQuantity(request.quantity);
    }
    if (request.country !== undefined && request.country !== null) {
      await newOrderPage.inputCountry(request.country);
    }
    if (request.couponCode) {
      await newOrderPage.inputCouponCode(request.couponCode);
    }
    await newOrderPage.clickPlaceOrder();

    const notificationResult = await newOrderPage.getResult();
    if (notificationResult.success) {
      const orderNumber = NewOrderPage.getOrderNumber(notificationResult.value);
      if (orderNumber) {
        return success({ orderNumber });
      }
      return failure({ message: 'Could not extract order number from success message', fieldErrors: [] });
    }
    return failure(notificationResult.error);
  }

  async viewOrder(request: ViewOrderRequest): Promise<Result<ViewOrderResponse, SystemError>> {
    const orderNumber = request.orderNumber;
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickOrderHistory();

    const orderHistoryPage = this.client.orderHistoryPage();
    await orderHistoryPage.inputOrderNumber(orderNumber);
    await orderHistoryPage.clickSearch();

    if (!(await orderHistoryPage.isOrderListed(orderNumber))) {
      return failure({ message: `Order ${orderNumber} does not exist.`, fieldErrors: [] });
    }

    await orderHistoryPage.clickViewOrderDetails(orderNumber);

    const detailsPage = this.client.orderDetailsPage();
    await detailsPage.waitForLoad();

    return success({
      orderNumber: await detailsPage.getOrderNumber(),
      orderTimestamp: await detailsPage.getOrderTimestamp(),
      sku: await detailsPage.getSku(),
      quantity: await detailsPage.getQuantity(),
      unitPrice: await detailsPage.getUnitPrice(),
      basePrice: await detailsPage.getBasePrice(),
      discountRate: await detailsPage.getDiscountRate(),
      discountAmount: await detailsPage.getDiscountAmount(),
      subtotalPrice: await detailsPage.getSubtotalPrice(),
      taxRate: await detailsPage.getTaxRate(),
      taxAmount: await detailsPage.getTaxAmount(),
      totalPrice: await detailsPage.getTotalPrice(),
      country: await detailsPage.getCountry(),
      appliedCouponCode: await detailsPage.getAppliedCouponCode(),
      status: await detailsPage.getStatus(),
    });
  }

  async cancelOrder(request: CancelOrderRequest): Promise<Result<CancelOrderResponse, SystemError>> {
    const orderNumber = request.orderNumber;
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickOrderHistory();

    const orderHistoryPage = this.client.orderHistoryPage();
    await orderHistoryPage.inputOrderNumber(orderNumber);
    await orderHistoryPage.clickSearch();

    if (!(await orderHistoryPage.isOrderListed(orderNumber))) {
      return failure({ message: `Order ${orderNumber} does not exist.`, fieldErrors: [] });
    }

    await orderHistoryPage.clickViewOrderDetails(orderNumber);

    const detailsPage = this.client.orderDetailsPage();
    await detailsPage.waitForLoad();
    await detailsPage.clickCancelOrder();

    const notificationResult = await detailsPage.getResult();
    if (notificationResult.success) return success({});
    return failure(notificationResult.error);
  }

  async deliverOrder(request: DeliverOrderRequest): Promise<Result<DeliverOrderResponse, SystemError>> {
    const orderNumber = request.orderNumber;
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickOrderHistory();

    const orderHistoryPage = this.client.orderHistoryPage();
    await orderHistoryPage.inputOrderNumber(orderNumber);
    await orderHistoryPage.clickSearch();

    if (!(await orderHistoryPage.isOrderListed(orderNumber))) {
      return failure({ message: `Order ${orderNumber} does not exist.`, fieldErrors: [] });
    }

    await orderHistoryPage.clickViewOrderDetails(orderNumber);

    const detailsPage = this.client.orderDetailsPage();
    await detailsPage.waitForLoad();
    await detailsPage.clickDeliverOrder();

    const notificationResult = await detailsPage.getResult();
    if (notificationResult.success) return success({});
    return failure(notificationResult.error);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<PublishCouponResponse, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickAdminCoupons();

    const couponPage = this.client.couponManagementPage();
    await couponPage.inputCouponCode(request.code);
    await couponPage.inputDiscountRate(request.discountRate);
    if (request.validFrom) {
      await couponPage.inputValidFrom(request.validFrom);
    }
    if (request.validTo) {
      await couponPage.inputValidTo(request.validTo);
    }
    if (request.usageLimit !== undefined && request.usageLimit !== null) {
      await couponPage.inputUsageLimit(Number(request.usageLimit));
    }
    await couponPage.clickPublishCoupon();

    const notificationResult = await couponPage.getResult();
    if (notificationResult.success) return success({});
    return failure(notificationResult.error);
  }

  async browseCoupons(_request: BrowseCouponsRequest): Promise<Result<BrowseCouponsResponse, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickAdminCoupons();

    const couponPage = this.client.couponManagementPage();
    await couponPage.clickRefreshCouponList();
    const rows = await couponPage.getCouponRows();

    return success({ coupons: rows });
  }

  async close(): Promise<void> {
    await this.client.close();
  }
}
