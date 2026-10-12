using System.Reflection;
using Common;
using Driver.Port;
using Driver.Port.Dtos;
using Driver.Port.Dtos.Error;
using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;
using Driver.Port.External.Tax;
using Driver.Port.External.Tax.Dtos;
using Driver.Port.External.Tax.Dtos.Error;
using Dsl.Core;
using Dsl.Core.External.Erp;
using Dsl.Core.External.Tax;
using Dsl.Core.Scenario;
using Dsl.Core.Shared;
using Dsl.Core.UseCase;
using Dsl.Port;

namespace SystemTests.Latest.DslTests;

// Runs without Docker: the DSL is wired to recording fake drivers.
public class DefaultGivensTest
{
    [Fact]
    public async Task OrderWithoutDeclaredProductShouldCreateDefaultProductAndTaxRateExactlyOnce()
    {
        var erp = new RecordingErpDriver();
        var tax = new RecordingTaxDriver();
        await using var app = CreateApp(erp, tax, new FakeMyShopDriver());

        await new ScenarioDsl(null, app)
            .Given().Order()
            .When().PlaceOrder().WithOrderNumber("WHEN-ORDER")
            .Then().ShouldSucceed();

        Assert.Equal((1, 1), (erp.ProductCalls.Count, tax.TaxRateCalls.Count));
    }

    private static UseCaseDsl CreateApp(IErpDriver erp, ITaxDriver tax, IMyShopDriver shop)
    {
        var configuration = new Configuration("http://ui", "http://api", "http://erp", "http://tax", "http://clock",
            null, ExternalSystemMode.Stub, ChannelMode.Static);
        var app = new UseCaseDsl(configuration);

        var context = (UseCaseContext)Field("_context").GetValue(app)!;
        Field("_erp").SetValue(app, new ErpDsl(erp, context));
        Field("_tax").SetValue(app, new TaxDsl(tax, context));

        var shops = (Dictionary<string, MyShopDsl>)Field("_shops").GetValue(app)!;
        shops["API"] = MyShopDsl.CreateAsync(shop, context).Result;
        return app;
    }

    private static FieldInfo Field(string name) =>
        typeof(UseCaseDsl).GetField(name, BindingFlags.Instance | BindingFlags.NonPublic)!;

    private sealed class RecordingErpDriver : IErpDriver
    {
        public List<ReturnsProductRequest> ProductCalls { get; } = new();

        public Task<Result<VoidValue, ErpErrorResponse>> GoToErpAsync() => Ok();

        public Task<Result<GetProductResponse, ErpErrorResponse>> GetProductAsync(GetProductRequest request) =>
            throw new NotSupportedException();

        public Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request)
        {
            ProductCalls.Add(request);
            return Ok();
        }

        public Task<Result<VoidValue, ErpErrorResponse>> ReturnsPromotionAsync(ReturnsPromotionRequest request) => Ok();

        private static Task<Result<VoidValue, ErpErrorResponse>> Ok() =>
            Task.FromResult(Result<VoidValue, ErpErrorResponse>.Success(VoidValue.Empty));

        public void Dispose() { }
        public ValueTask DisposeAsync() => ValueTask.CompletedTask;
    }

    private sealed class RecordingTaxDriver : ITaxDriver
    {
        public List<ReturnsTaxRateRequest> TaxRateCalls { get; } = new();

        public Task<Result<VoidValue, TaxErrorResponse>> GoToTaxAsync() => Ok();

        public Task<Result<GetTaxResponse, TaxErrorResponse>> GetTaxRateAsync(string? country) =>
            throw new NotSupportedException();

        public Task<Result<VoidValue, TaxErrorResponse>> ReturnsTaxRateAsync(ReturnsTaxRateRequest request)
        {
            TaxRateCalls.Add(request);
            return Ok();
        }

        private static Task<Result<VoidValue, TaxErrorResponse>> Ok() =>
            Task.FromResult(Result<VoidValue, TaxErrorResponse>.Success(VoidValue.Empty));

        public void Dispose() { }
    }

    private sealed class FakeMyShopDriver : IMyShopDriver
    {
        public void ActAs(UserIdentity identity) { }

        public Task<Result<GoToMyShopResponse, SystemError>> GoToMyShopAsync(GoToMyShopRequest request) =>
            throw new NotSupportedException();

        public Task<Result<PlaceOrderResponse, SystemError>> PlaceOrderAsync(PlaceOrderRequest request) =>
            Task.FromResult(Result<PlaceOrderResponse, SystemError>.Success(
                new PlaceOrderResponse { OrderNumber = "ORD-1" }));

        public Task<Result<CancelOrderResponse, SystemError>> CancelOrderAsync(CancelOrderRequest request) =>
            throw new NotSupportedException();

        public Task<Result<DeliverOrderResponse, SystemError>> DeliverOrderAsync(DeliverOrderRequest request) =>
            throw new NotSupportedException();

        public Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(ViewOrderRequest request) =>
            throw new NotSupportedException();

        public Task<Result<BrowseOrderHistoryResponse, SystemError>> BrowseOrderHistoryAsync(BrowseOrderHistoryRequest request) =>
            throw new NotSupportedException();

        public Task<Result<PublishCouponResponse, SystemError>> PublishCouponAsync(PublishCouponRequest request) =>
            Task.FromResult(Result<PublishCouponResponse, SystemError>.Success(new PublishCouponResponse()));

        public Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync(BrowseCouponsRequest request) =>
            throw new NotSupportedException();

        public ValueTask DisposeAsync() => ValueTask.CompletedTask;
    }
}
