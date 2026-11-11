import { useEffect, useState } from 'react';
import { Layout, LoadingSpinner } from '../components';
import { DEFAULT_COUNTRY } from '../hooks/useOrderForm';
import { profileService } from '../services/profile-service';
import { NewOrder } from './NewOrder';

/**
 * `/new-order`: Quick order with its Country prefilled from the customer's saved profile (UC-001 1a, UC-010 BR-5).
 * The form only appears once the profile is read, so a prefill can never overwrite what the customer types.
 */
export function QuickOrder() {
  const [country, setCountry] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    void profileService.viewProfile().then((result) => {
      if (ignore) return;
      const saved = result.success ? (result.data.country?.trim() ?? '') : '';
      setCountry(saved === '' ? DEFAULT_COUNTRY : saved);
    });
    return () => {
      ignore = true;
    };
  }, []);

  if (country === null) {
    return (
      <Layout title="Quick order" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Quick order' }]}>
        <div className="market-panel">
          <LoadingSpinner message="Loading..." />
        </div>
      </Layout>
    );
  }
  return <NewOrder initialCountry={country} />;
}
