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
        private readonly Func<Task>? _givenSetup;

        public WhenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario, bool hasProduct, bool hasPromotion, Func<Task>? givenSetup = null)
            : base(channel)
        {
            _app = app;
            _scenario = scenario;
            _hasProduct = hasProduct;
            _hasPromotion = hasPromotion;
            _givenSetup = givenSetup;
        }

        public WhenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario)
            : this(channel, app, scenario, false, false, null)
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
        }

        public GoToShop GoToShop()
        {
            return new GoToShop(_app, _scenario, () => EnsureGiven());
        }

        IGoToShop IWhenStage.GoToShop() => GoToShop();

        public PlaceOrder PlaceOrder()
        {
            return new PlaceOrder(_app, _scenario, () => EnsureGiven());
        }

        IPlaceOrder IWhenStage.PlaceOrder() => PlaceOrder();

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


