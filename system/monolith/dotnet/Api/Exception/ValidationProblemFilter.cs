using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ModelBinding;

namespace MyCompany.MyShop.Monolith.Api.Exception;

public class ValidationProblemFilter : IActionFilter
{
    public void OnActionExecuting(ActionExecutingContext context)
    {
        if (!context.ModelState.IsValid)
        {
            JsonElement? requestBody = null;

            // Try to read the raw request body to check original values
            try
            {
                context.HttpContext.Request.Body.Position = 0;
                using var reader = new StreamReader(context.HttpContext.Request.Body, leaveOpen: true);
                var body = reader.ReadToEndAsync().GetAwaiter().GetResult();
                if (!string.IsNullOrEmpty(body))
                {
                    requestBody = JsonSerializer.Deserialize<JsonElement>(body);
                }
            }
            catch
            {
                // Ignore - we'll fall back to default behavior
            }

            var errors = context.ModelState
                .Where(e => e.Value?.Errors.Count > 0)
                .Where(e => !string.Equals(e.Key, "request", StringComparison.OrdinalIgnoreCase))
                .Where(e => !string.Equals(e.Key, "$", StringComparison.OrdinalIgnoreCase))
                .SelectMany(e => BuildErrors(e.Key, e.Value!, requestBody))
                .ToList();

            var problemDetail = new
            {
                type = "https://api.my-company.com/errors/validation-error",
                title = "Validation Error",
                status = 422,
                detail = "The request contains one or more validation errors",
                timestamp = DateTime.UtcNow,
                errors
            };

            // Emit RFC 7807 application/problem+json (matching GlobalExceptionHandler and the
            // Java/TS providers). A plain ObjectResult would content-negotiate to application/json.
            context.Result = new ContentResult
            {
                StatusCode = StatusCodes.Status422UnprocessableEntity,
                ContentType = "application/problem+json",
                Content = JsonSerializer.Serialize(problemDetail)
            };
        }
    }

    public void OnActionExecuted(ActionExecutedContext context)
    {
    }

    private static IEnumerable<object> BuildErrors(string key, ModelStateEntry entry, JsonElement? requestBody)
    {
        var fieldName = NormalizeFieldName(key);
        var capitalizedField = char.ToUpper(fieldName[0]) + fieldName[1..];

        foreach (var err in entry.Errors)
        {
            var message = err.ErrorMessage;
            var isTypeMismatch = string.IsNullOrEmpty(message) ||
                                 message.Contains("could not be converted");

            if (isTypeMismatch)
            {
                // Check if the original JSON value was empty/whitespace
                if (IsRawValueEmptyOrWhitespace(fieldName, requestBody))
                {
                    yield return new
                    {
                        field = fieldName,
                        message = $"{capitalizedField} must not be empty",
                        code = (string?)null,
                        rejectedValue = (object?)null
                    };
                }
                else
                {
                    yield return new
                    {
                        field = fieldName,
                        message = $"{capitalizedField} must be an integer",
                        code = "TYPE_MISMATCH",
                        rejectedValue = (object?)null
                    };
                }
            }
            else
            {
                yield return new
                {
                    field = fieldName,
                    message,
                    code = (string?)null,
                    rejectedValue = (object?)null
                };
            }
        }
    }

    private static bool IsRawValueEmptyOrWhitespace(string fieldName, JsonElement? requestBody)
    {
        if (requestBody == null) return false;

        try
        {
            if (requestBody.Value.TryGetProperty(fieldName, out var prop))
            {
                if (prop.ValueKind == JsonValueKind.String)
                {
                    var rawValue = prop.GetString();
                    return string.IsNullOrWhiteSpace(rawValue);
                }
                if (prop.ValueKind == JsonValueKind.Null)
                {
                    return true;
                }
            }
        }
        catch
        {
            // Ignore
        }

        return false;
    }

    /// <summary>JSON path of the field, camelCase per segment: <c>ShippingAddress.FirstName</c> -> <c>shippingAddress.firstName</c>.</summary>
    private static string NormalizeFieldName(string key)
    {
        if (key.StartsWith("$."))
            key = key[2..];

        return string.Join('.', key.Split('.').Select(segment =>
            segment.Length > 0 ? char.ToLower(segment[0]) + segment[1..] : segment));
    }
}
