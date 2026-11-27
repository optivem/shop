import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { ProductImage } from '../../components/ProductImage';

describe('product image', () => {
  it('shows the ERP photo from the product imageUrl, lazy-loaded on cards', () => {
    render(<ProductImage imageUrl="/api/products/HP-15/image" alt="HP Laptop 15" lazy />);
    const photo = screen.getByRole('img', { name: 'HP Laptop 15' });
    expect(photo).toHaveAttribute('src', '/api/products/HP-15/image');
    expect(photo).toHaveAttribute('loading', 'lazy');
  });

  it('falls back to a neutral placeholder when the photo fails to load', () => {
    render(<ProductImage imageUrl="/api/products/HP-15/image" alt="HP Laptop 15" />);
    fireEvent.error(screen.getByRole('img', { name: 'HP Laptop 15' }));
    const placeholder = screen.getByRole('img', { name: 'HP Laptop 15' });
    expect(placeholder.tagName).toBe('DIV');
  });

  it('falls back to the placeholder when the product has no imageUrl', () => {
    const { container } = render(<ProductImage imageUrl={null} alt="" shape="square" />);
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('.product-image-placeholder')).toHaveAttribute('aria-hidden', 'true');
  });
});
