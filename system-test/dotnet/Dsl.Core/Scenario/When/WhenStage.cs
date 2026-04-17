using Dsl.Core.Scenario.When.Steps;
using Dsl.Port.When;
using Dsl.Port.When.Steps;
using Optivem.Testing;
using Driver.Adapter;
using static Dsl.Core.Gherkin.GherkinDefaults;

namespace Dsl.Core.Scenario.When
{
    public class WhenStage : BaseClause, IWhenStage
    {
        private readonly UseCaseDsl _app;
        private readonly ScenarioDsl _scenario;
        private bool _hasPromotion;
        private bool _hasProduct;
        private bool _hasTaxRate;
        private readonly Func<Task>? _givenSetup;

        public WhenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario, bool hasProduct, bool hasPromotion, bool hasTaxRate, Func<Task>? givenSetup = null)
            : base(channel)
        {
            _app = app;
            _scenario = scenario;
            _hasProduct = hasProduct;
            _hasPromotion = hasPromotion;
            _hasTaxRate = hasTaxRate;
            _givenSetup = givenSetup;
        }

        public WhenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario)
            : this(channel, app, scenario, false, false, false, null)
        {
        }

        private async Task EnsureGiven()
        {
            if (_givenSetup != null)
            {
                await _givenSetup();
            }

            if (!_hasPromotion)
            {
                var result = await _app.Erp().ReturnsPromotion()
                    .WithActive(DefaultPromotionActive)
                    .WithDiscount(DefaultPromotionDiscount)
                    .Execute();
                result.ShouldSucceed();
                _hasPromotion = true;
            }

            if (!_hasProduct)
            {
                var result = await _app.Erp().ReturnsProduct()
                    .Sku(DefaultSku)
                    .UnitPrice(DefaultUnitPrice)
                    .Execute();
                result.ShouldSucceed();
                _hasProduct = true;
            }

            if (!_hasTaxRate)
            {
                var result = await _app.Tax().ReturnsTaxRate()
                    .Country(DefaultCountry)
                    .TaxRate(DefaultTaxRate)
                    .Execute();
                result.ShouldSucceed();
                _hasTaxRate = true;
            }
        }

        public PlaceOrder PlaceOrder()
        {
            return new PlaceOrder(_app, _scenario, () => EnsureGiven());
        }

        IPlaceOrder IWhenStage.PlaceOrder() => PlaceOrder();

        public CancelOrder CancelOrder()
        {
            return new CancelOrder(_app, _scenario, () => EnsureGiven());
        }

        ICancelOrder IWhenStage.CancelOrder() => CancelOrder();

        public ViewOrder ViewOrder()
        {
            return new ViewOrder(_app, _scenario, () => EnsureGiven());
        }

        IViewOrder IWhenStage.ViewOrder() => ViewOrder();

        public WhenPublishCoupon PublishCoupon()
        {
            return new WhenPublishCoupon(_app, _scenario, () => EnsureGiven());
        }

        IPublishCoupon IWhenStage.PublishCoupon() => PublishCoupon();

        public WhenBrowseCoupons BrowseCoupons()
        {
            return new WhenBrowseCoupons(_app, _scenario, () => EnsureGiven());
        }

        IBrowseCoupons IWhenStage.BrowseCoupons() => BrowseCoupons();
    }
}
