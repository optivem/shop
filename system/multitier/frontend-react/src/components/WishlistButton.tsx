import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Heart } from 'lucide-react';

interface WishlistButtonProps {
  title: string;
  className?: string;
}

/**
 * The wishlist heart (Retail look). Visual only for now: there is no wishlist in the backend yet, so the
 * toggle lives in this button's state and is gone on reload.
 */
export function WishlistButton({ title, className = '' }: Readonly<WishlistButtonProps>) {
  const { t } = useTranslation();
  const [saved, setSaved] = useState(false);
  return (
    <button
      type="button"
      className={`wishlist-button ${className}`.trim()}
      aria-label={t('wishlist.save', { title })}
      aria-pressed={saved}
      onClick={() => setSaved((value) => !value)}
    >
      <Heart size={18} strokeWidth={2} aria-hidden="true" fill={saved ? 'currentColor' : 'none'} />
    </button>
  );
}
