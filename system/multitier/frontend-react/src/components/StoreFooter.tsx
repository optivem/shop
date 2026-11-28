import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';

/** Static footer: Shop, Help, the copyright note (storefront-ux §3). */
export function StoreFooter() {
  const { t } = useTranslation();
  const { products } = useCatalog();
  return (
    <footer className="store-footer mt-5">
      <div className="container py-4">
        <div className="row g-4">
          <div className="col-6 col-md-4">
            <h2 className="store-footer-heading">{t('footer.shop')}</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <Link to="/">{t('footer.allProducts')}</Link>
              </li>
              {categoriesOf(products).map((category) => (
                <li key={category}>
                  <Link to={`/?category=${encodeURIComponent(category)}`}>{category}</Link>
                </li>
              ))}
            </ul>
          </div>
          <div className="col-6 col-md-4">
            <h2 className="store-footer-heading">{t('footer.help')}</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <a href="#shipping">{t('footer.shipping')}</a>
              </li>
              <li>
                <a href="#returns">{t('footer.returns')}</a>
              </li>
              <li>
                <a href="#contact">{t('footer.contact')}</a>
              </li>
            </ul>
          </div>
          <div className="col-md-4 text-md-end">
            <p className="store-footer-brand mb-1">
              My<span className="store-logo-mark">Shop</span>
            </p>
            <p className="mb-0">{t('footer.tagline')}</p>
          </div>
        </div>
      </div>
    </footer>
  );
}
