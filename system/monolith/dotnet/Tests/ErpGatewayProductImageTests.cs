using System.Net;
using System.Net.Http.Headers;
using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services.External;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The ERP gateway's product image fetch, with the ERP scripted at the HTTP boundary: the ERP's
/// image path is resolved against the ERP base URL and the bytes come back with their content type.
/// </summary>
public class ErpGatewayProductImageTests
{
    private static (ErpGateway Gateway, ImageHandler Erp) Gateway(HttpStatusCode status)
    {
        var erp = new ImageHandler(status);
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["Erp:Url"] = "http://erp.test/erp" })
            .Build();
        return (new ErpGateway(new HttpClient(erp), configuration), erp);
    }

    [Fact]
    public async Task FetchesTheImageFromThePathTheErpGaveWithItsContentType()
    {
        var (gateway, erp) = Gateway(HttpStatusCode.OK);

        var image = await gateway.GetProductImageAsync("/erp/images/HP-15.jpg");

        Assert.NotNull(image);
        Assert.Equal("http://erp.test/erp/images/HP-15.jpg", erp.LastUri?.ToString());
        Assert.Equal("image/jpeg", image.ContentType);
        Assert.Equal(new byte[] { 1, 2, 3 }, image.Content);
    }

    [Fact]
    public async Task ReturnsNullWhenTheErpHasNoSuchImage()
    {
        var (gateway, _) = Gateway(HttpStatusCode.NotFound);

        Assert.Null(await gateway.GetProductImageAsync("/erp/images/UNKNOWN.jpg"));
    }

    [Fact]
    public async Task ThrowsErpUnavailableOnServerError()
    {
        var (gateway, _) = Gateway(HttpStatusCode.InternalServerError);

        await Assert.ThrowsAsync<ErpUnavailableException>(() => gateway.GetProductImageAsync("/erp/images/HP-15.jpg"));
    }

    /// <summary>Answers every request with the given status (an image when OK) and remembers the URI it was asked for.</summary>
    private sealed class ImageHandler(HttpStatusCode status) : HttpMessageHandler
    {
        public Uri? LastUri { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            LastUri = request.RequestUri;
            var ok = status == HttpStatusCode.OK;
            var content = new ByteArrayContent(ok ? new byte[] { 1, 2, 3 } : Array.Empty<byte>());
            content.Headers.ContentType = new MediaTypeHeaderValue(ok ? "image/jpeg" : "text/plain");
            return Task.FromResult(new HttpResponseMessage(status) { Content = content });
        }
    }
}
