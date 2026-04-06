import { Layout } from '../components';
import { CouponForm, CouponTable } from '../features/coupons';
import { useCoupons } from '../hooks';
import { useNotificationContext } from '../contexts/NotificationContext';

export function AdminCoupons() {
  const {
    coupons,
    isLoading,
    isCreating,
    submitCoupon,
    generateCouponCode,
    getCouponStatus,
    refresh
  } = useCoupons();

  const { setSuccess, handleResult } = useNotificationContext();

  const handleCouponSubmit = async (formData: any) => {
    const createdCode = formData.code;

    handleResult(await submitCoupon(formData), () => {
      setSuccess(`Coupon '${createdCode}' created successfully!`);
    });
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
        getCouponStatus={getCouponStatus}
        onRefresh={refresh}
      />
    </Layout>
  );
}
