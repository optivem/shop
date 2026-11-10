using Common;
using Driver.Port.External.Tax.Dtos;
using Driver.Port.External.Tax.Dtos.Error;

namespace Driver.Port.External.Tax;

public interface ITaxDriver : IDisposable, IAsyncDisposable
{
    Task<Result<VoidValue, TaxErrorResponse>> GoToTaxAsync();
    Task<Result<GetTaxResponse, TaxErrorResponse>> GetTaxRateAsync(string? country);
    Task<Result<VoidValue, TaxErrorResponse>> ReturnsTaxRateAsync(ReturnsTaxRateRequest request);
}
