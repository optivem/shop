namespace Driver.Port.External.Identity.Dtos.Error;

public class IdentityProviderErrorResponse
{
    public string? Message { get; set; }

    public override string ToString()
    {
        return Message ?? string.Empty;
    }
}
