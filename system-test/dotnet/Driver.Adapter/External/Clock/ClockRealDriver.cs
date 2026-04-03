using Driver.Port.External.Clock;
using Driver.Port.External.Clock.Dtos;
using Driver.Adapter.External.Clock.Client;
using Driver.Adapter.External.Clock.Client.Dtos;
using Driver.Adapter.External.Clock.Client.Dtos.Error;
using Common;

namespace Driver.Adapter.External.Clock;

public class ClockRealDriver : IClockDriver
{
    private readonly ClockRealClient _client;
    private bool _disposed;

    public ClockRealDriver()
    {
        _client = new ClockRealClient();
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    public ValueTask DisposeAsync()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
        return ValueTask.CompletedTask;
    }

    protected virtual void Dispose(bool disposing)
    {
        if (_disposed) return;
        _disposed = true;
    }

    public Task<Result<VoidValue, ClockErrorResponse>> GoToClockAsync()
        => ClockRealClient.CheckHealthAsync().MapErrorAsync(MapError);

    public Task<Result<GetTimeResponse, ClockErrorResponse>> GetTimeAsync()
        => _client.GetTimeAsync().MapAsync(MapResponse).MapErrorAsync(MapError);

    public Task<Result<VoidValue, ClockErrorResponse>> ReturnsTimeAsync(ReturnsTimeRequest request)
    {
        return Task.FromResult(Result.Success<ClockErrorResponse>());
    }

    private static GetTimeResponse MapResponse(ExtGetTimeResponse response)
        => new GetTimeResponse { Time = response.Time };

    private static ClockErrorResponse MapError(ExtClockErrorResponse errorResponse)
        => new ClockErrorResponse { Message = errorResponse.Message };
}

