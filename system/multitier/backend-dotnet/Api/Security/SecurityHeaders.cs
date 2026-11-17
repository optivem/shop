namespace MyCompany.MyShop.Backend.Api.Security;

/// <summary>
/// Security headers on every response (B-PRG 4.3), the twin of the Java backends' Spring Security header config.
/// The API answers JSON; the only HTML it serves is Swagger UI, which needs inline script/style and data: images.
/// HSTS goes on every response because TLS ends at the Cloud Run front and the app itself only sees http.
/// </summary>
public static class SecurityHeaders
{
    public const string ContentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'; "
        + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
        + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'";

    /// <summary>
    /// Registered first: the headers are written when the response starts, so they survive the exception
    /// handler clearing the response and cover static files too.
    /// </summary>
    public static IApplicationBuilder UseShopSecurityHeaders(this IApplicationBuilder app)
    {
        return app.Use(async (context, next) =>
        {
            context.Response.OnStarting(() =>
            {
                var headers = context.Response.Headers;
                headers["Content-Security-Policy"] = ContentSecurityPolicy;
                headers["Referrer-Policy"] = "strict-origin-when-cross-origin";
                headers["Strict-Transport-Security"] = "max-age=31536000";
                headers["X-Content-Type-Options"] = "nosniff";
                headers["X-Frame-Options"] = "DENY";
                return Task.CompletedTask;
            });
            await next();
        });
    }
}
