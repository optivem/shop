import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';

/**
 * The home hero and two promo tiles. They are the only places that show stock photos
 * (public/images/promo/, credited in StoreFooter's PHOTO_CREDITS). Each tile links to a
 * category the catalog actually carries.
 */
const SIDE_TILES = [
  { label: 'Shop monitors', path: '/?category=Monitors', image: '/images/promo/monitor.jpg', alt: 'Monitor on a desk' },
  { label: 'Build your desk setup', path: '/?category=Accessories', image: '/images/promo/keyboard.jpg', alt: 'Mechanical keyboard' },
];

export function PromoTiles() {
  return (
    <section className="promo-tiles mb-4" aria-label="Promotions">
      <div className="promo-hero">
        <img className="promo-photo promo-photo-hero" src="/images/promo/laptop.jpg" alt="Laptop on a white desk" />
        <div className="promo-hero-panel">
          <span className="badge badge-deal">New season</span>
          <h1 className="promo-hero-title">New season laptops</h1>
          <p className="lead">Free shipping over $1,000</p>
          <Link to="/?category=Laptops" className="btn btn-primary">
            Shop laptops
          </Link>
        </div>
      </div>
      <div className="promo-side">
        {SIDE_TILES.map((tile) => (
          <Link key={tile.path} to={tile.path} className="promo-tile">
            <img className="promo-photo" src={tile.image} alt={tile.alt} loading="lazy" />
            <span className="promo-tile-label">
              {tile.label} <ArrowRight size={16} aria-hidden="true" />
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
