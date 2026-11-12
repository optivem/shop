using System.Text.RegularExpressions;
using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>
/// The profile `/account` (My account › Profile &amp; address, UC-010): the name, the email from the sign-in account
/// (read-only), the default shipping address and its country select, and a note on how tax is charged. Saving shows a
/// notification: "Your profile has been saved.", or the validation error as <c>field: message</c> lines.
/// </summary>
public class ProfilePage : BasePage
{
    private const string SaveProfileSelector = "button[aria-label='Save Profile']";

    /// <summary>The profile has loaded (the single-page UIs read it first).</summary>
    public const string ReadySelector = SaveProfileSelector;

    private const string FirstNameSelector = "input[aria-label='First Name']";
    private const string LastNameSelector = "input[aria-label='Last Name']";
    private const string EmailSelector = "input[aria-label='Email']";
    private const string StreetSelector = "input[aria-label='Street']";
    private const string PostalCodeSelector = "input[aria-label='Postal Code']";
    private const string CitySelector = "input[aria-label='City']";
    private const string CountrySelector = "select[aria-label='Profile Country']";
    private const string CountryOptionSelector = CountrySelector + " option";
    private const string TaxNoticeSelector = "[aria-label='Tax Country Hint']";

    public ProfilePage(PageClient pageClient) : base(pageClient)
    {
    }

    public Task WaitForLoadedAsync() => PageClient.WaitForVisibleAsync(ReadySelector);

    /// <summary>Enters every field; a country the select does not offer is added to it, as an edited form would send it.</summary>
    public async Task InputAsync(UpdateProfileRequest request)
    {
        await PageClient.FillAsync(FirstNameSelector, request.FirstName);
        await PageClient.FillAsync(LastNameSelector, request.LastName);
        await PageClient.FillAsync(StreetSelector, request.Street);
        await PageClient.FillAsync(PostalCodeSelector, request.PostalCode);
        await PageClient.FillAsync(CitySelector, request.City);
        await PageClient.SelectOptionByValueAddingIfMissingAsync(CountrySelector, request.Country ?? string.Empty);
    }

    public Task ClickSaveProfileAsync() => PageClient.ClickAsync(SaveProfileSelector);

    /// <summary>The profile as the page shows it now; an empty field reads as "".</summary>
    public async Task<ViewProfileResponse> ReadAsync()
    {
        await WaitForLoadedAsync();
        var emailReadOnly = await PageClient.ReadAttributeAsync(EmailSelector, "readonly") != null
            || await PageClient.ReadAttributeAsync(EmailSelector, "disabled") != null;
        var countryChoices = (await PageClient.ReadAllAttributesNoWaitAsync(CountryOptionSelector, "value"))
            .Where(value => !string.IsNullOrEmpty(value))
            .Select(value => value!)
            .ToList();
        var taxNotice = await PageClient.ReadTextContentAsync(TaxNoticeSelector);
        return new ViewProfileResponse
        {
            FirstName = await PageClient.ReadInputValueAsync(FirstNameSelector),
            LastName = await PageClient.ReadInputValueAsync(LastNameSelector),
            Email = await PageClient.ReadInputValueAsync(EmailSelector),
            Street = await PageClient.ReadInputValueAsync(StreetSelector),
            PostalCode = await PageClient.ReadInputValueAsync(PostalCodeSelector),
            City = await PageClient.ReadInputValueAsync(CitySelector),
            Country = await PageClient.ReadInputValueAsync(CountrySelector),
            EmailChangeable = !emailReadOnly,
            CountryChoices = countryChoices,
            TaxNotice = Regex.Replace(taxNotice.Trim(), @"\s+", " "),
        };
    }
}
