using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ModelBinding;

namespace Optivem.Starter.Monolith.Api.Exception;

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
                type = "https://api.optivem.com/errors/validation-error",
                title = "Validation Error",
                status = 422,
                detail = "The request contains one or more validation errors",
                timestamp = DateTime.UtcNow,
                errors
            };

            context.Result = new ObjectResult(problemDetail)
            {
                StatusCode = StatusCodes.Status422UnprocessableEntity
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

    private static string NormalizeFieldName(string key)
    {
        if (key.StartsWith("$."))
            key = key[2..];

        if (key.Length > 0)
            key = char.ToLower(key[0]) + key[1..];

        return key;
    }
}
