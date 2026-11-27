using System.Net;
using System.Text;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// Stands in for the ERP over HTTP: answers each request path with a canned status and JSON body,
/// and 404 for any path it was not given. Lets the real ErpGateway run without a live ERP.
/// </summary>
public sealed class StubErpHandler : HttpMessageHandler
{
    private readonly Dictionary<string, (HttpStatusCode Status, string Body)> _responses = new();

    private readonly Dictionary<string, (byte[] Body, string ContentType)> _images = new();

    /// <summary>Answers the path with a binary body (a product photo) and its content type.</summary>
    public StubErpHandler ReturnsImage(string pathSuffix, byte[] body, string contentType = "image/jpeg")
    {
        _images[pathSuffix] = (body, contentType);
        return this;
    }

    public StubErpHandler Returns(string pathSuffix, HttpStatusCode status, string body = "")
    {
        _responses[pathSuffix] = (status, body);
        return this;
    }

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        var path = request.RequestUri!.AbsolutePath;
        var image = _images.FirstOrDefault(r => path.EndsWith(r.Key, StringComparison.Ordinal));
        if (image.Key != null)
        {
            var photo = new ByteArrayContent(image.Value.Body);
            photo.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue(image.Value.ContentType);
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK) { Content = photo });
        }

        var match = _responses.FirstOrDefault(r => path.EndsWith(r.Key, StringComparison.Ordinal));
        var (status, body) = match.Key != null ? match.Value : (HttpStatusCode.NotFound, "");
        return Task.FromResult(new HttpResponseMessage(status)
        {
            Content = new StringContent(body, Encoding.UTF8, "application/json")
        });
    }
}
