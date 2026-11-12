namespace Driver.Port.Dtos;

public class ViewProfileResponse
{
    public string? FirstName { get; set; }
    public string? LastName { get; set; }
    public string? Email { get; set; }
    public string? Street { get; set; }
    public string? PostalCode { get; set; }
    public string? City { get; set; }
    public string? Country { get; set; }

    /// <summary>UI only: whether the email field can be edited (null on the API, which has no email input).</summary>
    public bool? EmailChangeable { get; set; }

    /// <summary>UI only: the countries the country select offers, as codes, without its empty "Select a country" choice.</summary>
    public List<string>? CountryChoices { get; set; }

    /// <summary>UI only: the note on how sales tax is charged.</summary>
    public string? TaxNotice { get; set; }

    /// <summary>UI only: the notification shown after saving, such as "Your profile has been saved.".</summary>
    public string? Message { get; set; }
}
