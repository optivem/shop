using Dsl.Core.Scenario.When.Steps;
using Dsl.Port.When;
using Dsl.Port.When.Steps;
using Optivem.Testing;
using Driver.Adapter;

namespace Dsl.Core.Scenario.When
{
    public class WhenStage : BaseClause, IWhenStage
    {
        private readonly UseCaseDsl _app;
        private readonly ScenarioDsl _scenario;
        private readonly DefaultGivens _defaults;
        private readonly Func<Task>? _givenSetup;

        public WhenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario, DefaultGivens defaults, Func<Task>? givenSetup = null)
            : base(channel)
        {
            _app = app;
            _scenario = scenario;
            _defaults = defaults;
            _givenSetup = givenSetup;
        }

        public WhenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario)
            : this(channel, app, scenario, new DefaultGivens(app, channel), null)
        {
        }

        private async Task EnsureGiven()
        {
            if (_givenSetup != null)
            {
                await _givenSetup();
            }

            await _defaults.EnsureAsync(whenStepRuns: true);
        }

        public PlaceOrder PlaceOrder()
        {
            return new PlaceOrder(_app, _scenario, () => EnsureGiven());
        }

        IWhenPlaceOrder IWhenStage.PlaceOrder() => PlaceOrder();

        public CancelOrder CancelOrder()
        {
            return new CancelOrder(_app, _scenario, () => EnsureGiven());
        }

        IWhenCancelOrder IWhenStage.CancelOrder() => CancelOrder();

        public DeliverOrder DeliverOrder()
        {
            return new DeliverOrder(_app, _scenario, () => EnsureGiven());
        }

        IWhenDeliverOrder IWhenStage.DeliverOrder() => DeliverOrder();

        public WhenBrowseOrderHistory BrowseOrderHistory()
        {
            return new WhenBrowseOrderHistory(_app, _scenario, () => EnsureGiven());
        }

        IWhenBrowseOrderHistory IWhenStage.BrowseOrderHistory() => BrowseOrderHistory();

        public ViewOrder ViewOrder()
        {
            return new ViewOrder(_app, _scenario, () => EnsureGiven());
        }

        IWhenViewOrder IWhenStage.ViewOrder() => ViewOrder();

        public WhenPublishCoupon PublishCoupon()
        {
            return new WhenPublishCoupon(_app, _scenario, () => EnsureGiven());
        }

        IWhenPublishCoupon IWhenStage.PublishCoupon() => PublishCoupon();

        public WhenBrowseCoupons BrowseCoupons()
        {
            return new WhenBrowseCoupons(_app, _scenario, () => EnsureGiven());
        }

        IWhenBrowseCoupons IWhenStage.BrowseCoupons() => BrowseCoupons();
    }
}
