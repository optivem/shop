import { Link } from 'react-router-dom';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';

/** Static footer: Shop, Help, and the demo-store note (storefront-ux §3). */
export function StoreFooter() {
  const { products } = useCatalog();
  return (
    <footer className="store-footer mt-5">
      <div className="container py-4">
        <div className="row g-4">
          <div className="col-md-4">
            <h2 className="h6">Shop</h2>
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
          <div className="col-md-4">
            <h2 className="h6">Help</h2>
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
            <p className="mb-0">© MyShop · demo store for the Optivem ATDD course</p>
          </div>
        </div>
      </div>
    </footer>
  );
}
