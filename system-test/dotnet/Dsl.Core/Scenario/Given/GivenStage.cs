using Dsl.Core.Scenario.When;
using Dsl.Core.Scenario.Then;
using Dsl.Port.Given;
using Dsl.Port.Given.Steps;
using Dsl.Port.Then;
using Dsl.Port.When;
using Driver.Adapter;
using Dsl.Core.Gherkin.Given;
using Optivem.Testing;

namespace Dsl.Core.Scenario.Given
{
    public class GivenStage : BaseClause, IGivenStage
    {
        private readonly UseCaseDsl _app;
        private readonly ScenarioDsl _scenario;
        private readonly List<GivenProduct> _products;
        private readonly List<GivenOrder> _orders;
        private readonly List<GivenCoupon> _coupons;
        private GivenClock? _clock;
        private GivenPromotion _promotion;

        public GivenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario)
            : base(channel)
        {
            _app = app;
            _scenario = scenario;
            _products = new List<GivenProduct>();
            _orders = new List<GivenOrder>();
            _coupons = new List<GivenCoupon>();
            _clock = null;
            _promotion = new GivenPromotion(this);
        }

        public GivenProduct Product()
        {
            var productBuilder = new GivenProduct(this);
            _products.Add(productBuilder);
            return productBuilder;
        }

        IGivenProduct IGivenStage.Product() => Product();

        public GivenOrder Order()
        {
            var orderBuilder = new GivenOrder(this);
            _orders.Add(orderBuilder);
            return orderBuilder;
        }

        IGivenOrder IGivenStage.Order() => Order();

        public GivenClock Clock()
        {
            _clock = new GivenClock(this);
            return _clock;
        }

        IGivenClock IGivenStage.Clock() => Clock();

        public GivenPromotion Promotion()
        {
            _promotion = new GivenPromotion(this);
            return _promotion;
        }

        IGivenPromotion IGivenStage.Promotion() => Promotion();

        public GivenCoupon Coupon()
        {
            var couponBuilder = new GivenCoupon(this);
            _coupons.Add(couponBuilder);
            return couponBuilder;
        }

        IGivenCoupon IGivenStage.Coupon() => Coupon();

        public WhenStage When()
        {
            return new WhenStage(Channel, _app, _scenario, _products.Any(), true, SetupGiven);
        }

        IWhenStage IGivenStage.When() => When();

        public ThenStageBase Then()
        {
            return new ThenStageBase(_app, SetupGiven);
        }

        IThenStage IGivenStage.Then() => Then();

        private async Task SetupGiven()
        {
            await SetupClock();
            await SetupPromotion();
            await SetupErp();
            await SetupShop();
            await SetupCoupons();
        }

        private async Task SetupPromotion()
        {
            await _promotion.Execute(_app);
        }

        private async Task SetupClock()
        {
            if (_clock != null)
            {
                await _clock.Execute(_app);
            }
        }

        private async Task SetupErp()
        {
            if (_orders.Any() && !_products.Any())
            {
                var defaultProduct = new GivenProduct(this);
                _products.Add(defaultProduct);
            }

            foreach (var product in _products)
            {
                await product.Execute(_app);
            }
        }

        private async Task SetupShop()
        {
            await SetupOrders();
        }

        private async Task SetupOrders()
        {
            foreach (var order in _orders)
            {
                await order.Execute(_app);
            }
        }

        private async Task SetupCoupons()
        {
            foreach (var coupon in _coupons)
            {
                await coupon.Execute(_app);
            }
        }
    }
}


