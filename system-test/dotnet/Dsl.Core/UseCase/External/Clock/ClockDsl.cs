using Dsl.Core.External.Clock.UseCases;
using Driver.Port.External.Clock;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Clock;

public class ClockDsl : IDisposable, IAsyncDisposable
{
    private readonly IClockDriver _driver;
    private readonly UseCaseContext _context;
    private bool _disposed;

    public ClockDsl(IClockDriver driver, UseCaseContext context)
    {
        _driver = driver;
        _context = context;
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    public async ValueTask DisposeAsync()
    {
        await _driver.DisposeAsync();
        _disposed = true;
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (_disposed) return;
        if (disposing)
            _driver?.Dispose();
        _disposed = true;
    }

    public GoToClock GoToClock() => new(_driver, _context);

    public ReturnsTime ReturnsTime() => new(_driver, _context);

    public GetTime GetTime() => new(_driver, _context);
}



