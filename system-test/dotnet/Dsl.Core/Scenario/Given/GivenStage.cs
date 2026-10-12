using Dsl.Core.Scenario.When;
using Dsl.Core.Scenario.Then;
using Dsl.Port.Given;
using Dsl.Port.Given.Steps;
using Dsl.Port.Then;
using Dsl.Port.When;
using Driver.Adapter;
using Driver.Port;
using Dsl.Core.Scenario.Given;
using Optivem.Testing;

namespace Dsl.Core.Scenario.Given
{
    public class GivenStage : BaseClause, IGivenStage
    {
        private readonly UseCaseDsl _app;
        private readonly ScenarioDsl _scenario;
        private readonly List<GivenProduct> _products;
        private readonly List<GivenOrder> _orders;
        private readonly List<GivenCountry> _countries;
        private readonly List<GivenCoupon> _coupons;
        private readonly DefaultGivens _defaults;
        private GivenClock? _clock;
        private GivenPromotion _promotion;
        private readonly CustomerAliases _customers = new();
        private Func<UserIdentity> _loggedIn = () => UserIdentity.Default;
        private bool _loggedInChosen;

        public GivenStage(Channel? channel, UseCaseDsl app, ScenarioDsl scenario)
            : base(channel)
        {
            _app = app;
            _scenario = scenario;
            _products = new List<GivenProduct>();
            _orders = new List<GivenOrder>();
            _countries = new List<GivenCountry>();
            _coupons = new List<GivenCoupon>();
            _clock = null;
            _promotion = new GivenPromotion(this);
            _defaults = new DefaultGivens(app, channel);
        }

        public GivenProduct Product()
        {
            _defaults.MarkProduct();
            var productBuilder = new GivenProduct(this);
            _products.Add(productBuilder);
            return productBuilder;
        }

        IGivenProduct IGivenStage.Product() => Product();

        public GivenOrder Order()
        {
            _defaults.MarkOrder();
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

        public GivenCountry Country()
        {
            _defaults.MarkTaxRate();
            var countryBuilder = new GivenCountry(this);
            _countries.Add(countryBuilder);
            return countryBuilder;
        }

        IGivenCountry IGivenStage.Country() => Country();

        public GivenPromotion Promotion()
        {
            _promotion = new GivenPromotion(this);
            return _promotion;
        }

        IGivenPromotion IGivenStage.Promotion() => Promotion();

        public GivenCoupon Coupon()
        {
            _defaults.MarkCoupon();
            var couponBuilder = new GivenCoupon(this);
            _coupons.Add(couponBuilder);
            return couponBuilder;
        }

        IGivenCoupon IGivenStage.Coupon() => Coupon();

        public GivenStage LoggedInAsCustomer()
        {
            _customers.ReserveDefaultCustomer();
            return LoggedInAs(CustomerAliases.DefaultCustomer);
        }

        IGivenStage IGivenStage.LoggedInAsCustomer() => LoggedInAsCustomer();

        public GivenStage LoggedInAsCustomer(string alias)
        {
            _customers.Register(alias);
            return LoggedInAs(() => _customers.Resolve(alias));
        }

        IGivenStage IGivenStage.LoggedInAsCustomer(string alias) => LoggedInAsCustomer(alias);

        public GivenStage LoggedInAsAdmin() => LoggedInAs(() => UserIdentity.Admin);

        IGivenStage IGivenStage.LoggedInAsAdmin() => LoggedInAsAdmin();

        public GivenStage NotLoggedIn() => LoggedInAs(() => UserIdentity.Anonymous);

        IGivenStage IGivenStage.NotLoggedIn() => NotLoggedIn();

        private GivenStage LoggedInAs(Func<UserIdentity> identity)
        {
            _loggedIn = identity;
            _loggedInChosen = true;
            return this;
        }

        /// <summary>Registers a customer alias for an order placed in this scenario.</summary>
        internal void RegisterCustomer(string alias) => _customers.Register(alias);

        /// <summary>Resolves the customer behind an alias; only valid once the scenario is being set up.</summary>
        internal UserIdentity ResolveCustomer(string alias) => _customers.Resolve(alias);

        /// <summary>The default customer (customer1).</summary>
        internal static UserIdentity DefaultCustomer() => CustomerAliases.DefaultCustomer();

        public WhenStage When()
        {
            return new WhenStage(Channel, _app, _scenario, _defaults, SetupGiven);
        }

        IWhenStage IGivenStage.When() => When();

        public ThenStageBase Then()
        {
            return new ThenStageBase(_app, SetupGiven);
        }

        IThenStage IGivenStage.Then() => Then();

        private async Task SetupGiven()
        {
            ReserveDefaultCustomerIfUsed();
            await SetupClock();
            await SetupPromotion();
            await SetupErp();
            await SetupTax();
            await SetupMyShop();
            _app.ActAs(_loggedIn());
        }

        /// <summary>Operations with no explicit customer run as the default customer, so customer1 must stay theirs.</summary>
        private void ReserveDefaultCustomerIfUsed()
        {
            if (!_loggedInChosen || _orders.Any(o => o.IsPlacedByDefaultCustomer))
            {
                _customers.ReserveDefaultCustomer();
            }
        }

        private async Task SetupPromotion()
        {
            await _promotion.Execute(_app);
            _defaults.MarkPromotion();
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
            foreach (var product in _products)
            {
                await product.Execute(_app);
            }
        }

        private async Task SetupTax()
        {
            foreach (var country in _countries)
            {
                await country.Execute(_app);
            }
        }

        private async Task SetupMyShop()
        {
            await SetupCoupons();
            await SetupOrders();
        }

        private async Task SetupCoupons()
        {
            await _defaults.EnsureAsync(whenStepRuns: false);

            foreach (var coupon in _coupons)
            {
                await coupon.Execute(_app);
            }
        }

        private async Task SetupOrders()
        {
            foreach (var order in _orders)
            {
                await order.Execute(_app);
            }
        }
    }
}
