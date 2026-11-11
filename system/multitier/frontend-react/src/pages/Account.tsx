import { useEffect, useState, type FormEvent } from 'react';
import { Lock } from 'lucide-react';
import { Layout, LoadingSpinner, SubmitButton } from '../components';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { AddressFields } from '../features/checkout/AddressFields';
import type { AddressLabels } from '../features/checkout/address-labels';
import { EMPTY_ADDRESS } from '../features/checkout/shipping';
import { profileService } from '../services/profile-service';
import type { ProfileResponse, ShippingAddress } from '../types/api.types';

/** Selectors of the profile form (storefront-ux §8). */
const PROFILE_LABELS: AddressLabels = {
  firstName: 'First Name',
  lastName: 'Last Name',
  street: 'Street',
  postalCode: 'Postal Code',
  city: 'City',
  country: 'Profile Country',
};

function toForm(profile: ProfileResponse): ShippingAddress {
  return {
    firstName: profile.firstName ?? '',
    lastName: profile.lastName ?? '',
    street: profile.street ?? '',
    postalCode: profile.postalCode ?? '',
    city: profile.city ?? '',
    country: profile.country ?? '',
  };
}

/** `/account` (My account › Profile & address): name and default shipping address (UC-010). */
export function Account() {
  const { handleResult, setSuccess, setError } = useNotificationContext();
  const [form, setForm] = useState<ShippingAddress | null>(null);
  const [email, setEmail] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let ignore = false;
    void profileService.viewProfile().then((result) => {
      if (ignore) return;
      if (result.success) {
        setForm(toForm(result.data));
        setEmail(result.data.email ?? '');
      } else {
        setError(result.error);
        setForm(EMPTY_ADDRESS);
      }
    });
    return () => {
      ignore = true;
    };
  }, [setError]);

  // The backend validates (required fields, spaces count as empty, US/DE/GB) and reports each field to correct.
  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!form) return;
    setIsSaving(true);
    const result = await profileService.updateProfile(form);
    setIsSaving(false);
    handleResult(result, (saved) => {
      setForm(toForm(saved));
      setEmail(saved.email ?? '');
      setSuccess('Your profile has been saved.');
    });
  };

  return (
    <Layout title="My account" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'My account' }, { label: 'Profile & address' }]}>
      <section className="market-panel account-panel" aria-labelledby="account-heading">
        <h1 id="account-heading" className="account-title">
          Profile &amp; address
        </h1>
        {form ? (
          <form className="account-form" noValidate onSubmit={(e) => void handleSubmit(e)}>
            <div className="row g-3">
              <div className="col-md-6">
                <label htmlFor="profile-firstName" className="form-label">
                  First name
                </label>
                <input
                  id="profile-firstName"
                  type="text"
                  className="form-control"
                  aria-label="First Name"
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="profile-lastName" className="form-label">
                  Last name
                </label>
                <input
                  id="profile-lastName"
                  type="text"
                  className="form-control"
                  aria-label="Last Name"
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                />
              </div>
              <div className="col-12">
                <label htmlFor="profile-email" className="form-label">
                  Email
                </label>
                <input id="profile-email" type="email" className="form-control" aria-label="Email" value={email} readOnly disabled />
                <div className="form-text d-flex align-items-center gap-1">
                  <Lock size={12} aria-hidden="true" className="flex-shrink-0" />
                  Your email comes from your sign-in account and can't be changed here.
                </div>
              </div>
            </div>
            <h2 className="account-section-title">Shipping address</h2>
            <AddressFields idPrefix="profile" address={form} errors={{}} labels={PROFILE_LABELS} onChange={setForm} withoutNames />
            <p className="form-text mt-3 mb-0" aria-label="Tax Country Hint">
              Sales tax is charged at the rate of your shipping address's country.
            </p>
            <div className="account-actions">
              <SubmitButton isSubmitting={isSaving} text="Save profile" loadingText="Saving..." ariaLabel="Save Profile" className="btn btn-primary" />
            </div>
          </form>
        ) : (
          <LoadingSpinner message="Loading profile..." />
        )}
      </section>
    </Layout>
  );
}
