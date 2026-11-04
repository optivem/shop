namespace Driver.Adapter.Api.Client.Controllers;

/// <summary>Builds a URL query string from the parameters that are set; null values are left out.</summary>
internal static class QueryString
{
    public static string Of(params (string Name, string? Value)[] parameters)
    {
        var query = string.Join("&", parameters
            .Where(parameter => parameter.Value != null)
            .Select(parameter => $"{parameter.Name}={Uri.EscapeDataString(parameter.Value!)}"));
        return query.Length == 0 ? string.Empty : "?" + query;
    }
}
