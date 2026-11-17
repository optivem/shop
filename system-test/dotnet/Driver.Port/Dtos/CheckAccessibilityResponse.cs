namespace Driver.Port.Dtos;

/// <summary>The accessibility check of the page that is open.</summary>
public class CheckAccessibilityResponse
{
    /// <summary>The serious and critical axe violations, one readable line each: rule, impact, help and the offending elements.</summary>
    public List<string> Violations { get; set; } = [];
}
