import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Lock } from 'lucide-react';
import { Layout, LoadingSpinner, SubmitButton } from '../components';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { AddressFields } from '../features/checkout/AddressFields';
import type { AddressLabels } from '../features/checkout/address-labels';
import { EMPTY_ADDRESS } from '../features/checkout/shipping';
import { profileService } from '../services/profile-service';
import type { ProfileResponse, ShippingAddress } from '../types/api.types';

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
  const { t } = useTranslation('account');
  const { handleResult, setSuccess, setError } = useNotificationContext();
  const [form, setForm] = useState<ShippingAddress | null>(null);
  const [email, setEmail] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  /** Selectors of the profile form (storefront-ux §8). */
  const profileLabels: AddressLabels = {
    firstName: t('profile.labels.firstName'),
    lastName: t('profile.labels.lastName'),
    street: t('profile.labels.street'),
    postalCode: t('profile.labels.postalCode'),
    city: t('profile.labels.city'),
    country: t('profile.labels.country'),
  };

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
      setSuccess(t('profile.saved'));
    });
  };

  return (
    <Layout
      title={t('profile.title')}
      breadcrumbs={[{ label: t('breadcrumbs.home'), path: '/' }, { label: t('breadcrumbs.myAccount') }, { label: t('breadcrumbs.profileAddress') }]}
    >
      <section className="market-panel account-panel" aria-labelledby="account-heading">
        <h1 id="account-heading" className="account-title">
          {t('profile.heading')}
        </h1>
        {form ? (
          <form className="account-form" noValidate onSubmit={(e) => void handleSubmit(e)}>
            <div className="row g-3">
              <div className="col-md-6">
                <label htmlFor="profile-firstName" className="form-label">
                  {t('profile.firstName')}
                </label>
                <input
                  id="profile-firstName"
                  type="text"
                  className="form-control"
                  aria-label={t('profile.labels.firstName')}
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="profile-lastName" className="form-label">
                  {t('profile.lastName')}
                </label>
                <input
                  id="profile-lastName"
                  type="text"
                  className="form-control"
                  aria-label={t('profile.labels.lastName')}
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                />
              </div>
              <div className="col-12">
                <label htmlFor="profile-email" className="form-label">
                  {t('profile.email')}
                </label>
                <input id="profile-email" type="email" className="form-control" aria-label={t('profile.labels.email')} value={email} readOnly disabled />
                <div className="form-text d-flex align-items-center gap-1">
                  <Lock size={12} aria-hidden="true" className="flex-shrink-0" />
                  {t('profile.emailNote')}
                </div>
              </div>
            </div>
            <h2 className="account-section-title">{t('profile.shippingAddress')}</h2>
            <AddressFields idPrefix="profile" address={form} errors={{}} labels={profileLabels} onChange={setForm} withoutNames />
            <p className="form-text mt-3 mb-0" aria-label={t('profile.labels.taxCountryHint')}>
              {t('profile.taxHint')}
            </p>
            <div className="account-actions">
              <SubmitButton isSubmitting={isSaving} text={t('profile.save')} loadingText={t('profile.saving')} ariaLabel={t('profile.labels.saveProfile')} className="btn btn-primary" />
            </div>
          </form>
        ) : (
          <LoadingSpinner message={t('profile.loading')} />
        )}
      </section>
    </Layout>
  );
}
