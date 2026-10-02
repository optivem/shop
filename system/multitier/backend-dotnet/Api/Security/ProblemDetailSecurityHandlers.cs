using System.Text.Json;

namespace MyCompany.MyShop.Backend.Api.Security;

/// <summary>Writes 401 / 403 as RFC 7807 problem details, matching the shape of the API's other errors.</summary>
public static class ProblemDetailSecurityHandlers
{
    private const string ProblemJsonContentType = "application/problem+json";
    private const string ErrorTypeBaseUri = "https://api.my-company.com/errors";

    public static Task WriteUnauthorizedAsync(HttpContext context)
    {
        context.Response.Headers.WWWAuthenticate = "Bearer";
        return WriteAsync(context, StatusCodes.Status401Unauthorized, "Unauthorized",
            "Authentication is required", "/unauthorized");
    }

    public static Task WriteForbiddenAsync(HttpContext context) =>
        WriteAsync(context, StatusCodes.Status403Forbidden, "Forbidden",
            "You do not have permission to perform this action", "/forbidden");

    private static Task WriteAsync(HttpContext context, int status, string title, string detail, string typePath)
    {
        context.Response.StatusCode = status;
        var problem = new
        {
            type = ErrorTypeBaseUri + typePath,
            title,
            status,
            detail,
            timestamp = DateTime.UtcNow,
        };
        return context.Response.WriteAsJsonAsync(
            problem, options: (JsonSerializerOptions?)null, contentType: ProblemJsonContentType);
    }
}
