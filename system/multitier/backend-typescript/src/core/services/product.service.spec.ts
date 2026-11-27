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
  imageUrl: '/erp/images/BOOK-123.jpg',
};

const EXPECTED_PRODUCT = {
  sku: 'BOOK-123',
  title: 'Clean Code',
  description: 'A handbook of agile software craftsmanship',
  brand: 'Prentice Hall',
  category: 'Books',
  price: new Decimal(12),
  imageUrl: '/api/products/BOOK-123/image',
};

describe('ProductService', () => {
  let service: ProductService;
  let erpGateway: jest.Mocked<
    Pick<ErpGateway, 'getProducts' | 'getProductDetails' | 'getProductImage'>
  >;

  beforeEach(() => {
    erpGateway = {
      getProducts: jest.fn(),
      getProductDetails: jest.fn(),
      getProductImage: jest.fn(),
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
        {
          ...EXPECTED_PRODUCT,
          sku: 'BOOK-456',
          price: new Decimal(15.5),
          imageUrl: '/api/products/BOOK-456/image',
        },
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

    it('has no image URL when the ERP has no image', async () => {
      erpGateway.getProductDetails.mockResolvedValue({
        ...ERP_PRODUCT,
        imageUrl: undefined,
      });

      await expect(service.viewProduct('BOOK-123')).resolves.toMatchObject({
        imageUrl: null,
      });
    });
  });

  describe('getProductImage', () => {
    const IMAGE = {
      content: Buffer.from([1, 2, 3]),
      contentType: 'image/jpeg',
    };

    it('proxies the ERP image', async () => {
      erpGateway.getProductDetails.mockResolvedValue(ERP_PRODUCT);
      erpGateway.getProductImage.mockResolvedValue(IMAGE);

      await expect(service.getProductImage('BOOK-123')).resolves.toBe(IMAGE);
      expect(erpGateway.getProductImage).toHaveBeenCalledWith(
        '/erp/images/BOOK-123.jpg',
      );
    });

    it('rejects an unknown SKU as not found', async () => {
      erpGateway.getProductDetails.mockResolvedValue(null);

      await expect(service.getProductImage('UNKNOWN')).rejects.toThrow(
        new NotExistValidationException('Product UNKNOWN does not exist.'),
      );
      expect(erpGateway.getProductImage).not.toHaveBeenCalled();
    });

    it('rejects a product without an image as not found', async () => {
      erpGateway.getProductDetails.mockResolvedValue({
        ...ERP_PRODUCT,
        imageUrl: undefined,
      });

      await expect(service.getProductImage('BOOK-123')).rejects.toThrow(
        new NotExistValidationException('Product BOOK-123 has no image.'),
      );
      expect(erpGateway.getProductImage).not.toHaveBeenCalled();
    });

    it('rejects an image missing on the ERP as not found', async () => {
      erpGateway.getProductDetails.mockResolvedValue(ERP_PRODUCT);
      erpGateway.getProductImage.mockResolvedValue(null);

      await expect(service.getProductImage('BOOK-123')).rejects.toThrow(
        new NotExistValidationException(
          'Image of product BOOK-123 does not exist.',
        ),
      );
    });
  });
});
