import { Controller, Get, Param } from '@nestjs/common';
import { ProductService } from '../../core/services/product.service';
import { ViewProductDetailsResponse } from '../../core/dtos/view-product-details-response.dto';

/** Public catalog: anonymous callers may browse and view products (see route-policy.ts). */
@Controller('api/products')
export class ProductController {
  constructor(private readonly productService: ProductService) {}

  @Get()
  async browseProducts(): Promise<ViewProductDetailsResponse[]> {
    return this.productService.browseProducts();
  }

  @Get(':sku')
  async viewProduct(
    @Param('sku') sku: string,
  ): Promise<ViewProductDetailsResponse> {
    return this.productService.viewProduct(sku);
  }
}
