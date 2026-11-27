export interface GetProductResponse {
  sku: string;
  title: string;
  description: string;
  category: string;
  brand: string;
  imageUrl?: string;
  price: number;
}
