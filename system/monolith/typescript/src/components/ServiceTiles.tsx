import { Icon, type IconName } from "./Icon";

/** The store's standing promises (the same ones the header strip makes). */
const SERVICES: { icon: IconName; title: string; detail: string }[] = [
  { icon: "truck", title: "Free shipping", detail: "on orders over $1,000" },
  { icon: "rotate-ccw", title: "30-day returns", detail: "send it back within a month" },
  { icon: "shield-check", title: "Secure checkout", detail: "signed in with your account" },
];

/** The home page shows the three promises in full; the product page shows a tighter version under the buy box. */
export function ServiceTiles({ compact = false }: Readonly<{ compact?: boolean }>) {
  return (
    <ul className={`service-tiles${compact ? " service-tiles-compact" : ""}`} aria-label="Our services">
      {SERVICES.map(({ icon, title, detail }) => (
        <li key={title} className="service-tile">
          <span className="service-tile-icon" aria-hidden="true">
            <Icon name={icon} size={compact ? 18 : 22} />
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
