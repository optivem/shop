namespace Driver.Port.Dtos;

/// <summary>A visitor's sign-up (UC-013): the new sign-in account and the shipping country chosen on the registration form.</summary>
public class SignUpRequest
{
    public string? Username { get; set; }
    public string? Password { get; set; }
    public string? Email { get; set; }
    public string? FirstName { get; set; }
    public string? LastName { get; set; }
    public string? Country { get; set; }

    public override string ToString() => $"SignUpRequest[username={Username}, email={Email}, country={Country}]";
}
