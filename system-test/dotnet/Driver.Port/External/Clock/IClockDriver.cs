using Driver.Port.External.Clock.Dtos;
using Common;

namespace Driver.Port.External.Clock;

public interface IClockDriver : IDisposable, IAsyncDisposable
{
    Task<Result<VoidValue, ClockErrorResponse>> GoToClockAsync();
    Task<Result<GetTimeResponse, ClockErrorResponse>> GetTimeAsync();
    Task<Result<VoidValue, ClockErrorResponse>> ReturnsTimeAsync(ReturnsTimeRequest request);
}

