import type { CatalogQuery } from './catalog-query';

interface CatalogFiltersProps {
  categories: string[];
  brands: string[];
  query: CatalogQuery;
  onChange: (query: CatalogQuery) => void;
}

/** Category (one, or All) and brand (any number) filters for the catalog. */
export function CatalogFilters({ categories, brands, query, onChange }: Readonly<CatalogFiltersProps>) {
  const toggleBrand = (brand: string, checked: boolean) => {
    const next = checked ? [...query.brands, brand] : query.brands.filter((b) => b !== brand);
    onChange({ ...query, brands: brands.filter((b) => next.includes(b)) });
  };

  return (
    <div className="catalog-filters">
      <fieldset className="mb-4" aria-label="Category Filter">
        <legend className="h6">Category</legend>
        {['', ...categories].map((category) => {
          const id = `category-${category || 'all'}`;
          return (
            <div className="form-check" key={id}>
              <input
                className="form-check-input"
                type="radio"
                name="category"
                id={id}
                value={category}
                checked={query.category === category}
                onChange={() => onChange({ ...query, category })}
              />
              <label className="form-check-label" htmlFor={id}>
                {category || 'All'}
              </label>
            </div>
          );
        })}
      </fieldset>
      <fieldset className="mb-4" aria-label="Brand Filter">
        <legend className="h6">Brand</legend>
        {brands.map((brand) => {
          const id = `brand-${brand}`;
          return (
            <div className="form-check" key={id}>
              <input
                className="form-check-input"
                type="checkbox"
                name="brand"
                id={id}
                value={brand}
                checked={query.brands.includes(brand)}
                onChange={(e) => toggleBrand(brand, e.target.checked)}
              />
              <label className="form-check-label" htmlFor={id}>
                {brand}
              </label>
            </div>
          );
        })}
      </fieldset>
    </div>
  );
}
