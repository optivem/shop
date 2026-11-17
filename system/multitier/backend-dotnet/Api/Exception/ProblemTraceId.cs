using System.Diagnostics;

namespace MyCompany.MyShop.Backend.Api.Exception;

/// <summary>
/// The trace id stamped on every problem-details body (<c>traceId</c>), so an error a user reports can be found in the
/// logs, where the same id is logged as <c>trace_id</c>. Falls back to the request's own identifier outside a span.
/// </summary>
public static class ProblemTraceId
{
    public static string Of(HttpContext httpContext) =>
        Activity.Current?.TraceId.ToHexString() ?? httpContext.TraceIdentifier;
}
