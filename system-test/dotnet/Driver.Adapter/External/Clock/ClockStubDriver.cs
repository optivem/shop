using Driver.Port.External.Clock;
using Driver.Port.External.Clock.Dtos;
using Driver.Adapter.External.Clock.Client;
using Driver.Adapter.External.Clock.Client.Dtos;
using Driver.Adapter.External.Clock.Client.Dtos.Error;
using Common;

namespace Driver.Adapter.External.Clock;

public class ClockStubDriver : IClockDriver
{
    private readonly ClockStubClient _client;
    private bool _disposed;

    public ClockStubDriver(string baseUrl)
    {
        _client = new ClockStubClient(baseUrl);
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    public async ValueTask DisposeAsync()
    {
        await _client.RemoveStubsAsync();
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (_disposed) return;
        if (disposing)
            _client?.Dispose();
        _disposed = true;
    }

    public Task<Result<VoidValue, ClockErrorResponse>> GoToClockAsync()
        => _client.CheckHealthAsync().MapErrorAsync(MapError);

    public Task<Result<GetTimeResponse, ClockErrorResponse>> GetTimeAsync()
        => _client.GetTimeAsync().MapAsync(MapResponse).MapErrorAsync(MapError);

    public async Task<Result<VoidValue, ClockErrorResponse>> ReturnsTimeAsync(ReturnsTimeRequest request)
    {
        var extResponse = new ExtGetTimeResponse
        {
            Time = DateTimeOffset.Parse(request.Time!, System.Globalization.CultureInfo.InvariantCulture)
        };
        await _client.ConfigureGetTimeAsync(extResponse);
        return Result.Success<ClockErrorResponse>();
    }

    private static GetTimeResponse MapResponse(ExtGetTimeResponse response)
        => new GetTimeResponse { Time = response.Time };

    private static ClockErrorResponse MapError(ExtClockErrorResponse errorResponse)
        => new ClockErrorResponse { Message = errorResponse.Message };
}

