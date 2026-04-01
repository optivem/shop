using Microsoft.AspNetCore.Diagnostics;
using Optivem.Starter.Monolith.Core.Exceptions;

namespace Optivem.Starter.Monolith.Api.Exception;

public class GlobalExceptionHandler : IExceptionHandler
{
    private const string ValidationErrorTypeUri = "https://api.optivem.com/errors/validation-error";
    private const string ResourceNotFoundTypeUri = "https://api.optivem.com/errors/resource-not-found";
    private const string InternalServerErrorTypeUri = "https://api.optivem.com/errors/internal-server-error";

    private readonly ILogger<GlobalExceptionHandler> _logger;

    public GlobalExceptionHandler(ILogger<GlobalExceptionHandler> logger)
    {
        _logger = logger;
    }

    public async ValueTask<bool> TryHandleAsync(HttpContext httpContext, System.Exception exception, CancellationToken cancellationToken)
    {
        switch (exception)
        {
            case NotExistValidationException notExistEx:
                await WriteNotExistResponse(httpContext, notExistEx, cancellationToken);
                return true;

            case ValidationException validationEx:
                await WriteValidationResponse(httpContext, validationEx, cancellationToken);
                return true;

            default:
                await WriteInternalServerErrorResponse(httpContext, exception, cancellationToken);
                return true;
        }
    }

    private static async Task WriteValidationResponse(HttpContext httpContext, ValidationException ex, CancellationToken cancellationToken)
    {
        httpContext.Response.StatusCode = StatusCodes.Status422UnprocessableEntity;
        httpContext.Response.ContentType = "application/problem+json";

        object response;

        if (ex.FieldName != null)
        {
            response = new
            {
                type = ValidationErrorTypeUri,
                title = "Validation Error",
                status = 422,
                detail = "The request contains one or more validation errors",
                timestamp = DateTime.UtcNow,
                errors = new[]
                {
                    new { field = ex.FieldName, message = ex.Message }
                }
            };
        }
        else
        {
            response = new
            {
                type = ValidationErrorTypeUri,
                title = "Validation Error",
                status = 422,
                detail = ex.Message,
                timestamp = DateTime.UtcNow
            };
        }

        await httpContext.Response.WriteAsJsonAsync(response, cancellationToken);
    }

    private static async Task WriteNotExistResponse(HttpContext httpContext, NotExistValidationException ex, CancellationToken cancellationToken)
    {
        httpContext.Response.StatusCode = StatusCodes.Status404NotFound;
        httpContext.Response.ContentType = "application/problem+json";

        var response = new
        {
            type = ResourceNotFoundTypeUri,
            title = "Resource Not Found",
            status = 404,
            detail = ex.Message,
            timestamp = DateTime.UtcNow
        };

        await httpContext.Response.WriteAsJsonAsync(response, cancellationToken);
    }

    private async Task WriteInternalServerErrorResponse(HttpContext httpContext, System.Exception ex, CancellationToken cancellationToken)
    {
        _logger.LogError(ex, "Unexpected error occurred");

        httpContext.Response.StatusCode = StatusCodes.Status500InternalServerError;
        httpContext.Response.ContentType = "application/problem+json";

        var response = new
        {
            type = InternalServerErrorTypeUri,
            title = "Internal Server Error",
            status = 500,
            detail = $"Internal server error: {ex.Message}",
            timestamp = DateTime.UtcNow
        };

        await httpContext.Response.WriteAsJsonAsync(response, cancellationToken);
    }
}
