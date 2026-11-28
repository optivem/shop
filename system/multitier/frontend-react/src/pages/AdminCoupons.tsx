import { useTranslation } from 'react-i18next';
import { Layout } from '../components';
import { CouponForm, CouponTable, type CouponFormData } from '../features/coupons';
import { useCoupons } from '../hooks';
import { useNotificationContext } from '../contexts/useNotificationContext';

export function AdminCoupons() {
  const { t } = useTranslation('account');
  const {
    coupons,
    isLoading,
    error,
    isCreating,
    submitCoupon,
    generateCouponCode,
    getCouponStatus,
    refresh
  } = useCoupons();

  const { setSuccess, handleResult } = useNotificationContext();

  const handleCouponSubmit = async (formData: CouponFormData): Promise<boolean> => {
    const createdCode = formData.code;
    const result = await submitCoupon(formData);

    handleResult(result, () => {
      setSuccess(t('adminCoupons.created', { code: createdCode }));
    });

    return result.success;
  };

  return (
    <Layout
      title={t('adminCoupons.title')}
      breadcrumbs={[{ label: t('breadcrumbs.home'), path: '/' }, { label: t('breadcrumbs.couponManagement') }]}
    >
      <h1 className="admin-coupons-title">{t('adminCoupons.heading')}</h1>
      <CouponForm
        onSubmit={handleCouponSubmit}
        isSubmitting={isCreating}
        generateCouponCode={generateCouponCode}
      />

      <CouponTable
        coupons={coupons}
        isLoading={isLoading}
        error={error}
        getCouponStatus={getCouponStatus}
        onRefresh={refresh}
      />
    </Layout>
  );
}
