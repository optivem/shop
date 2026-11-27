import { RotateCcw, ShieldCheck, Truck } from 'lucide-react';

/** The store's standing promises (the same ones the header strip makes). */
const SERVICES = [
  { icon: Truck, title: 'Free shipping', detail: 'on orders over $1,000' },
  { icon: RotateCcw, title: '30-day returns', detail: 'send it back within a month' },
  { icon: ShieldCheck, title: 'Secure checkout', detail: 'signed in with your account' },
];

interface ServiceTilesProps {
  /** The product page shows a tighter version under the buy box. */
  compact?: boolean;
}

export function ServiceTiles({ compact = false }: Readonly<ServiceTilesProps>) {
  return (
    <ul className={`service-tiles${compact ? ' service-tiles-compact' : ''}`} aria-label="Our services">
      {SERVICES.map(({ icon: Icon, title, detail }) => (
        <li key={title} className="service-tile">
          <span className="service-tile-icon" aria-hidden="true">
            <Icon size={compact ? 18 : 22} strokeWidth={2} />
          </span>
          <span>
            <strong className="service-tile-title">{title}</strong>
            <span className="service-tile-detail">{detail}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}
