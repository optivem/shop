import { Controller, Get, Param, StreamableFile } from '@nestjs/common';
import { ProductService } from '../../core/services/product.service';
import { ViewProductDetailsResponse } from '../../core/dtos/view-product-details-response.dto';

/** Public catalog: anonymous callers may browse and view products and their photos (see route-policy.ts). */
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

  /** The product photo, proxied from the ERP so an img tag can load it from the shop's own origin. */
  @Get(':sku/image')
  async viewProductImage(@Param('sku') sku: string): Promise<StreamableFile> {
    const image = await this.productService.getProductImage(sku);
    return new StreamableFile(image.content, { type: image.contentType });
  }
}
