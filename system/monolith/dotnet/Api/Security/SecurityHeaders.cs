namespace MyCompany.MyShop.Monolith.Api.Security;

/// <summary>
/// Security headers on every response (B-PRG 4.3), the twin of the Java backends' Spring Security header config.
/// The pages carry inline scripts and style attributes, hence 'unsafe-inline'; everything they load (Bootstrap, the
/// self-hosted font, images) is served from this app. form-action is left out on purpose: browsers apply it to the
/// redirect to Keycloak that follows the logout form.
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
