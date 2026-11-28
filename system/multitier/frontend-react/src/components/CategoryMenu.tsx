import { useTranslation } from 'react-i18next';
import { useEffect, useId, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { categoriesOf } from '../features/catalog/catalog-query';
import type { Product } from '../types/api.types';

interface CategoryMenuProps {
  products: Product[];
}

/**
 * The header's "Categories" button (Retail look): a disclosure panel with one link per catalog category,
 * each with a round photo of a product in it. It sits outside the "Store navigation" row on purpose, so the
 * row's category links (a UI-driver hook) stay the only ones there.
 */
export function CategoryMenu({ products }: Readonly<CategoryMenuProps>) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const categories = categoriesOf(products);

  useEffect(() => {
    if (!open) return undefined;
    const onPointer = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('pointerdown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const close = () => setOpen(false);
  const photoOf = (category: string) => products.find((p) => p.category === category && p.imageUrl)?.imageUrl;

  return (
    <div className="category-menu" ref={rootRef}>
      <button
        type="button"
        className="category-menu-button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((value) => !value)}
      >
        {open ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
        <span className="category-menu-label">{t('categoryMenu.categories')}</span>
      </button>
      <div id={panelId} className="category-menu-panel" hidden={!open}>
        <ul className="list-unstyled mb-0">
          <li>
            <Link to="/" className="category-menu-link" onClick={close}>
              <span className="category-menu-photo category-menu-photo-all" aria-hidden="true" />
              {t('categoryMenu.allProducts')}
            </Link>
          </li>
          {categories.map((category) => {
            const photo = photoOf(category);
            return (
              <li key={category}>
                <Link to={`/?category=${encodeURIComponent(category)}`} className="category-menu-link" onClick={close}>
                  {photo ? (
                    <img className="category-menu-photo" src={photo} alt="" width={40} height={40} loading="lazy" />
                  ) : (
                    <span className="category-menu-photo" aria-hidden="true" />
                  )}
                  {category}
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
