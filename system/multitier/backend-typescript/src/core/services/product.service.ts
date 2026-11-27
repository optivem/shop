import { Injectable } from '@nestjs/common';
import Decimal from 'decimal.js';
import { ViewProductDetailsResponse } from '../dtos/view-product-details-response.dto';
import { ErpProductDetailsResponse } from '../dtos/external/erp-product-details-response.dto';
import type { ErpProductImage } from '../dtos/external/erp-product-image.dto';
import { NotExistValidationException } from '../exceptions/not-exist-validation.exception';
import { ErpGateway } from './external/erp.gateway';

/** Products are owned by the ERP; MyShop only proxies its catalog. */
@Injectable()
export class ProductService {
  constructor(private readonly erpGateway: ErpGateway) {}

  async browseProducts(): Promise<ViewProductDetailsResponse[]> {
    const products = await this.erpGateway.getProducts();
    return products.map((product) => toResponse(product, product.id ?? ''));
  }

  async viewProduct(sku: string): Promise<ViewProductDetailsResponse> {
    const product = await this.erpGateway.getProductDetails(sku);
    if (!product) {
      throw new NotExistValidationException(`Product ${sku} does not exist.`);
    }
    return toResponse(product, sku);
  }

  // The photo is proxied rather than linked: the ERP is not reachable from the browser.
  async getProductImage(sku: string): Promise<ErpProductImage> {
    const product = await this.erpGateway.getProductDetails(sku);
    if (!product) {
      throw new NotExistValidationException(`Product ${sku} does not exist.`);
    }
    if (!product.imageUrl?.trim()) {
      throw new NotExistValidationException(`Product ${sku} has no image.`);
    }
    const image = await this.erpGateway.getProductImage(product.imageUrl);
    if (!image) {
      throw new NotExistValidationException(
        `Image of product ${sku} does not exist.`,
      );
    }
    return image;
  }
}

function toResponse(
  product: ErpProductDetailsResponse,
  fallbackSku: string,
): ViewProductDetailsResponse {
  const sku = product.id ?? fallbackSku;
  return {
    sku,
    title: product.title ?? null,
    description: product.description ?? null,
    brand: product.brand ?? null,
    category: product.category ?? null,
    price: new Decimal(product.price),
    imageUrl: product.imageUrl?.trim() ? `/api/products/${sku}/image` : null,
  };
}
