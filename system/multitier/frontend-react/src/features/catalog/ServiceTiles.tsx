import { useTranslation } from 'react-i18next';
import { RotateCcw, ShieldCheck, Truck } from 'lucide-react';

/** The store's standing promises (the same ones the header strip makes). */
const SERVICES = [
  { icon: Truck, key: 'freeShipping' },
  { icon: RotateCcw, key: 'returns' },
  { icon: ShieldCheck, key: 'secure' },
] as const;

interface ServiceTilesProps {
  /** The product page shows a tighter version under the buy box. */
  compact?: boolean;
}

export function ServiceTiles({ compact = false }: Readonly<ServiceTilesProps>) {
  const { t } = useTranslation('catalog');
  return (
    <ul className={`service-tiles${compact ? ' service-tiles-compact' : ''}`} aria-label={t('serviceTiles.aria')}>
      {SERVICES.map(({ icon: Icon, key }) => (
        <li key={key} className="service-tile">
          <span className="service-tile-icon" aria-hidden="true">
            <Icon size={compact ? 18 : 22} strokeWidth={2} />
          </span>
          <span>
            <strong className="service-tile-title">{t(`serviceTiles.${key}Title`)}</strong>
            <span className="service-tile-detail">{t(`serviceTiles.${key}Detail`)}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}
