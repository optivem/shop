using System.Diagnostics;
using Microsoft.AspNetCore.Http;
using MyCompany.MyShop.Backend.Api.Exception;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Unit;

public class ProblemTraceIdTest
{
    [Fact]
    public void UsesTheCurrentActivitysTraceId()
    {
        using var activity = new Activity("request").Start();

        Assert.Equal(activity.TraceId.ToHexString(), ProblemTraceId.Of(new DefaultHttpContext()));
    }

    [Fact]
    public void FallsBackToTheRequestIdentifierOutsideASpan()
    {
        var previous = Activity.Current;
        Activity.Current = null;
        try
        {
            var context = new DefaultHttpContext { TraceIdentifier = "request-1" };

            Assert.Equal("request-1", ProblemTraceId.Of(context));
        }
        finally
        {
            Activity.Current = previous;
        }
    }
}
