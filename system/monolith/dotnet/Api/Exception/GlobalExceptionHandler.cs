using System.Text.Json;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Monolith.Core.Exceptions;

namespace MyCompany.MyShop.Monolith.Api.Exception;

public class GlobalExceptionHandler : IExceptionHandler
{
    private const string ProblemJsonContentType = "application/problem+json";

    private const string ValidationErrorTypeUri = "https://api.my-company.com/errors/validation-error";
    private const string ResourceNotFoundTypeUri = "https://api.my-company.com/errors/resource-not-found";
    private const string ConflictTypeUri = "https://api.my-company.com/errors/conflict";
    private const string InternalServerErrorTypeUri = "https://api.my-company.com/errors/internal-server-error";
    private const string ServiceUnavailableTypeUri = "https://api.my-company.com/errors/service-unavailable";
    private const string ErpUnavailableDetail = "Product information can't be retrieved right now";
    // The exception is logged server-side only. Its message can carry internals (SQL, connection
    // strings, stack fragments), so the client gets a generic detail instead.
    private const string GeneralErrorDetail = "An unexpected error occurred. Please try again later.";

    private readonly ILogger<GlobalExceptionHandler> _logger;
    // Tells clients when to retry: the ERP circuit breaker's break duration, after which it lets calls through again.
    private readonly string _erpRetryAfterSeconds;

    public GlobalExceptionHandler(ILogger<GlobalExceptionHandler> logger, IConfiguration configuration)
    {
        _logger = logger;
        _erpRetryAfterSeconds = System.Math.Max(1, (int)System.Math.Ceiling(configuration.GetValue("Resilience:Erp:BreakDurationMilliseconds", 10000) / 1000.0)).ToString(System.Globalization.CultureInfo.InvariantCulture);
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

            case ErpUnavailableException erpEx:
                await WriteErpUnavailableResponse(httpContext, erpEx, cancellationToken);
                return true;

            case DbUpdateConcurrencyException:
                await WriteConflictResponse(httpContext, cancellationToken);
                return true;

            default:
                await WriteInternalServerErrorResponse(httpContext, exception, cancellationToken);
                return true;
        }
    }

    private static async Task WriteValidationResponse(HttpContext httpContext, ValidationException ex, CancellationToken cancellationToken)
    {
        httpContext.Response.StatusCode = StatusCodes.Status422UnprocessableEntity;

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

        await httpContext.Response.WriteAsJsonAsync(
            response, options: (JsonSerializerOptions?)null, contentType: ProblemJsonContentType, cancellationToken);
    }

    private static async Task WriteNotExistResponse(HttpContext httpContext, NotExistValidationException ex, CancellationToken cancellationToken)
    {
        httpContext.Response.StatusCode = StatusCodes.Status404NotFound;

        var response = new
        {
            type = ResourceNotFoundTypeUri,
            title = "Resource Not Found",
            status = 404,
            detail = ex.Message,
            timestamp = DateTime.UtcNow
        };

        await httpContext.Response.WriteAsJsonAsync(
            response, options: (JsonSerializerOptions?)null, contentType: ProblemJsonContentType, cancellationToken);
    }

    private static async Task WriteConflictResponse(HttpContext httpContext, CancellationToken cancellationToken)
    {
        httpContext.Response.StatusCode = StatusCodes.Status409Conflict;

        var response = new
        {
            type = ConflictTypeUri,
            title = "Conflict",
            status = 409,
            detail = "The resource was modified concurrently, please retry",
            timestamp = DateTime.UtcNow
        };

        await httpContext.Response.WriteAsJsonAsync(
            response, options: (JsonSerializerOptions?)null, contentType: ProblemJsonContentType, cancellationToken);
    }

    private async Task WriteErpUnavailableResponse(HttpContext httpContext, ErpUnavailableException ex, CancellationToken cancellationToken)
    {
        // The ERP failure details (URL, status, body) are logged server-side only; the client is
        // told product information is temporarily unavailable.
        _logger.LogError(ex, "ERP unavailable");

        httpContext.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
        httpContext.Response.Headers.RetryAfter = _erpRetryAfterSeconds;

        var response = new
        {
            type = ServiceUnavailableTypeUri,
            title = "Service Unavailable",
            status = 503,
            detail = ErpUnavailableDetail,
            timestamp = DateTime.UtcNow
        };

        await httpContext.Response.WriteAsJsonAsync(
            response, options: (JsonSerializerOptions?)null, contentType: ProblemJsonContentType, cancellationToken);
    }

    private async Task WriteInternalServerErrorResponse(HttpContext httpContext, System.Exception ex, CancellationToken cancellationToken)
    {
        _logger.LogError(ex, "Unexpected error occurred");

        httpContext.Response.StatusCode = StatusCodes.Status500InternalServerError;

        var response = new
        {
            type = InternalServerErrorTypeUri,
            title = "Internal Server Error",
            status = 500,
            detail = GeneralErrorDetail,
            timestamp = DateTime.UtcNow
        };

        await httpContext.Response.WriteAsJsonAsync(
            response, options: (JsonSerializerOptions?)null, contentType: ProblemJsonContentType, cancellationToken);
    }
}
