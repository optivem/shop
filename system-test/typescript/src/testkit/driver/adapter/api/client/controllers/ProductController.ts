import type { Result } from '../../../../../common/result.js';
import { success } from '../../../../../common/result.js';
import type { CatalogProductDto } from '../../../../port/dtos/BrowseCatalogResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import type { BearerTokenSource } from '../../../shared/client/http/bearer-token-source.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

/** The API's product shape: the price is a JSON number. */
interface ProductJson {
  sku: string;
  title: string;
  brand: string;
  category: string;
  description: string;
  price: number;
}

export class ProductController {
  private static readonly ENDPOINT = '/api/products';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string, tokenSource?: BearerTokenSource) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse, tokenSource);
  }

  async browseProducts(): Promise<Result<CatalogProductDto[], SystemError>> {
    const result = await this.httpClient.get<ProductJson[]>(ProductController.ENDPOINT);
    if (!result.success) return result;
    return success(result.value.map((product) => ({ ...product, price: String(product.price) })));
  }

  async viewProduct(sku: string): Promise<Result<CatalogProductDto, SystemError>> {
    const result = await this.httpClient.get<ProductJson>(`${ProductController.ENDPOINT}/${encodeURIComponent(sku)}`);
    if (!result.success) return result;
    return success({ ...result.value, price: String(result.value.price) });
  }
}
