import Decimal from 'decimal.js';
import { NotExistValidationException } from '../exceptions/not-exist-validation.exception';
import { ErpUnavailableException } from '../exceptions/erp-unavailable.exception';
import type { ErpGateway } from './external/erp.gateway';
import { ProductService } from './product.service';

const ERP_PRODUCT = {
  id: 'BOOK-123',
  title: 'Clean Code',
  description: 'A handbook of agile software craftsmanship',
  price: 12,
  category: 'Books',
  brand: 'Prentice Hall',
};

const EXPECTED_PRODUCT = {
  sku: 'BOOK-123',
  title: 'Clean Code',
  description: 'A handbook of agile software craftsmanship',
  brand: 'Prentice Hall',
  category: 'Books',
  price: new Decimal(12),
};

describe('ProductService', () => {
  let service: ProductService;
  let erpGateway: jest.Mocked<
    Pick<ErpGateway, 'getProducts' | 'getProductDetails'>
  >;

  beforeEach(() => {
    erpGateway = {
      getProducts: jest.fn(),
      getProductDetails: jest.fn(),
    };
    service = new ProductService(erpGateway as unknown as ErpGateway);
  });

  describe('browseProducts', () => {
    it('proxies the ERP catalog, including the descriptive fields', async () => {
      erpGateway.getProducts.mockResolvedValue([
        ERP_PRODUCT,
        { ...ERP_PRODUCT, id: 'BOOK-456', price: 15.5 },
      ]);

      await expect(service.browseProducts()).resolves.toEqual([
        EXPECTED_PRODUCT,
        { ...EXPECTED_PRODUCT, sku: 'BOOK-456', price: new Decimal(15.5) },
      ]);
    });

    it('returns an empty list when the ERP has no products', async () => {
      erpGateway.getProducts.mockResolvedValue([]);

      await expect(service.browseProducts()).resolves.toEqual([]);
    });

    it('propagates ERP unavailability', async () => {
      erpGateway.getProducts.mockRejectedValue(
        new ErpUnavailableException('down'),
      );

      await expect(service.browseProducts()).rejects.toBeInstanceOf(
        ErpUnavailableException,
      );
    });
  });

  describe('viewProduct', () => {
    it('proxies the ERP product', async () => {
      erpGateway.getProductDetails.mockResolvedValue(ERP_PRODUCT);

      await expect(service.viewProduct('BOOK-123')).resolves.toEqual(
        EXPECTED_PRODUCT,
      );
      expect(erpGateway.getProductDetails).toHaveBeenCalledWith('BOOK-123');
    });

    it('rejects an unknown SKU as not found', async () => {
      erpGateway.getProductDetails.mockResolvedValue(null);

      const result = service.viewProduct('UNKNOWN');
      await expect(result).rejects.toBeInstanceOf(NotExistValidationException);
      await expect(result).rejects.toThrow('Product UNKNOWN does not exist.');
    });
  });
});
