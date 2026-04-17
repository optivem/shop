import type { Browser } from 'playwright';
import type { Result } from '../../../../common/result.js';
import { success, failure } from '../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../port/shop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../port/shop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../port/shop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../port/shop/dtos/SystemError.js';
import type { PublishCouponRequest } from '../../../port/shop/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../port/shop/dtos/BrowseCouponsResponse.js';
import type { ShopDriver } from '../../../port/shop/shop-driver.js';
import { ShopUiClient } from './client/ShopUiClient.js';
import { NewOrderPage } from './client/pages/NewOrderPage.js';

export class ShopUiDriver implements ShopDriver {
  private readonly client: ShopUiClient;

  constructor(baseUrl: string, browser: Browser) {
    this.client = new ShopUiClient(baseUrl, browser);
  }

  async goToShop(): Promise<Result<void, SystemError>> {
    const result = await this.client.openHomePage();
    if (result.success) return success(undefined);
    return failure(result.error);
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickNewOrder();

    const newOrderPage = this.client.newOrderPage();
    await newOrderPage.inputSku(request.sku);
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

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
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

  async cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
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
    if (notificationResult.success) return success(undefined);
    return failure(notificationResult.error);
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
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
    if (notificationResult.success) return success(undefined);
    return failure(notificationResult.error);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
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
    if (notificationResult.success) return success(undefined);
    return failure(notificationResult.error);
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
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
