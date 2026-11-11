import { Link } from 'react-router-dom';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';

/**
 * Unsplash photographers of the photos that ship in public/images/promo/ (home hero and promo tiles).
 * Keep in sync with docs/design/home-redesign/images/SOURCES.md: add a row whenever a new photo ships.
 */
const PHOTO_CREDITS = [
  { photographer: 'Dhony Koswara', url: 'https://unsplash.com/photos/btVTuAIhvk8' }, // laptop.jpg
  { photographer: 'Huy Phan', url: 'https://unsplash.com/photos/VXpeQ3GetDU' }, // monitor.jpg
  { photographer: 'Jay Zhang', url: 'https://unsplash.com/photos/U1I0IoQx3Yo' }, // keyboard.jpg
];

/** Static footer: Shop, Help, the demo-store note and the photo credits (storefront-ux §3). */
export function StoreFooter() {
  const { products } = useCatalog();
  return (
    <footer className="store-footer mt-5">
      <div className="container py-4">
        <div className="row g-4">
          <div className="col-6 col-md-4">
            <h2 className="store-footer-heading">Shop</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <Link to="/">All products</Link>
              </li>
              {categoriesOf(products).map((category) => (
                <li key={category}>
                  <Link to={`/?category=${encodeURIComponent(category)}`}>{category}</Link>
                </li>
              ))}
            </ul>
          </div>
          <div className="col-6 col-md-4">
            <h2 className="store-footer-heading">Help</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <a href="#shipping">Shipping</a>
              </li>
              <li>
                <a href="#returns">Returns</a>
              </li>
              <li>
                <a href="#contact">Contact</a>
              </li>
            </ul>
          </div>
          <div className="col-md-4 text-md-end">
            <p className="store-footer-brand mb-1">
              MyShop<span className="store-logo-mark" aria-hidden="true">.</span>
            </p>
            <p className="mb-0">© MyShop · demo store for the Optivem ATDD course</p>
          </div>
        </div>
      </div>
      <div className="store-footer-bottom">
        <div className="container py-3">
          <p className="mb-0">
            Photos on Unsplash by{' '}
            {PHOTO_CREDITS.map((credit, index) => (
              <span key={credit.url}>
                {index > 0 && ', '}
                <a href={credit.url} target="_blank" rel="noopener noreferrer">
                  {credit.photographer}
                </a>
              </span>
            ))}
          </p>
        </div>
      </div>
    </footer>
  );
}
