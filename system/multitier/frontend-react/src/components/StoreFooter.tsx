import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useCatalog } from '../contexts/useCatalog';
import { categoriesOf } from '../features/catalog/catalog-query';

/**
 * Unsplash photographers of every photo the shop shows: the promo photos in public/images/promo/ (home hero,
 * docs/design/home-redesign/images/SOURCES.md) and the ERP product photos (external-systems/erp-images/SOURCES.md).
 * One entry per photographer (several shot more than one photo), linking to their Unsplash profile.
 * Keep in sync with both SOURCES.md files: add a photographer whenever a photo by someone new ships.
 */
const PHOTO_CREDITS = [
  { photographer: 'Dhony Koswara', url: 'https://unsplash.com/@dhony24' }, // promo laptop.jpg, HP-15
  { photographer: 'Lasse Jensen', url: 'https://unsplash.com/@maybejensen' }, // DELL-XPS, ANKER-HUB-7
  { photographer: 'Faraaz Zuberi', url: 'https://unsplash.com/@ffz_20' }, // LENOVO-T14
  { photographer: '2H Media', url: 'https://unsplash.com/@2hmedia' }, // ASUS-ZENBOOK-14
  { photographer: 'Huy Phan', url: 'https://unsplash.com/@huyphan2602' }, // promo monitor.jpg, DELL-U2723QE
  { photographer: 'Roberto Nickson', url: 'https://unsplash.com/@rpnickson' }, // LG-27UL500
  { photographer: 'Nikita Kachanovsky', url: 'https://unsplash.com/@nkachanovskyyy' }, // SAMSUNG-S24
  { photographer: 'Ravi Palwe', url: 'https://unsplash.com/@ravipalwe' }, // LOGI-MX3S
  { photographer: 'Jay Zhang', url: 'https://unsplash.com/@jay_zhang' }, // promo keyboard.jpg, LOGI-MXKEYS
];

/** Static footer: Shop, Help, the demo-store note and the photo credits (storefront-ux §3). */
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
      <div className="store-footer-bottom">
        <div className="container py-3">
          <p className="mb-0">
            {t('footer.photosBy')}{' '}
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
