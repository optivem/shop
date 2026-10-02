import { ComponentHarness } from '../support/component-harness';

/**
 * Publish + browse coupon flows. Publish returns 204 No Content with no body — this is the real
 * contract the system tests verify. Mirrors the Java CouponComponentTest.
 */
describe('Coupon (component)', () => {
  const harness = new ComponentHarness();

  beforeAll(async () => {
    await harness.start();
  }, 120_000);

  afterAll(async () => {
    await harness.stop();
  }, 60_000);

  beforeEach(async () => {
    await harness.resetState();
  });

  it('publish returns No Content, then browse lists the coupon', async () => {
    const publish = await harness
      .adminApi()
      .post('/api/coupons')
      .send({ code: 'SAVE10', discountRate: 0.2, usageLimit: 100 });
    expect(publish.status).toBe(204);

    const browse = await harness.adminApi().get('/api/coupons');
    expect(browse.status).toBe(200);
    const body = browse.body as { coupons: { code: string }[] };
    const codes = body.coupons.map((c) => c.code);
    expect(codes).toContain('SAVE10');
  });
});
