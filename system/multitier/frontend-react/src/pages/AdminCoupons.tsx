import { Layout } from '../components';
import { CouponForm, CouponTable, type CouponFormData } from '../features/coupons';
import { useCoupons } from '../hooks';
import { useNotificationContext } from '../contexts/useNotificationContext';

export function AdminCoupons() {
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
      setSuccess(`Coupon '${createdCode}' created successfully!`);
    });

    return result.success;
  };

  return (
    <Layout
      title="Coupon Management"
      breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Coupon Management' }]}
    >
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
