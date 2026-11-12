using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

public class ViewProfileVerification : ResponseVerification<ViewProfileResponse>
{
    public ViewProfileVerification(ViewProfileResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public ViewProfileVerification FirstName(string? expectedFirstName)
    {
        Response.FirstName.ShouldBe(expectedFirstName,
            $"Expected first name to be '{expectedFirstName}', but was '{Response.FirstName}'");
        return this;
    }

    public ViewProfileVerification LastName(string? expectedLastName)
    {
        Response.LastName.ShouldBe(expectedLastName,
            $"Expected last name to be '{expectedLastName}', but was '{Response.LastName}'");
        return this;
    }

    public ViewProfileVerification Email(string? expectedEmail)
    {
        Response.Email.ShouldBe(expectedEmail,
            $"Expected email to be '{expectedEmail}', but was '{Response.Email}'");
        return this;
    }

    public ViewProfileVerification Street(string? expectedStreet)
    {
        Response.Street.ShouldBe(expectedStreet,
            $"Expected street to be '{expectedStreet}', but was '{Response.Street}'");
        return this;
    }

    /// <summary>The street, postal code, city and country are all empty (an empty field reads as null or "").</summary>
    public ViewProfileVerification AddressIsEmpty()
    {
        string?[] address = [Response.Street, Response.PostalCode, Response.City, Response.Country];
        address.ShouldAllBe(value => string.IsNullOrEmpty(value),
            $"Expected no saved address, but the profile has street '{Response.Street}', postal code "
            + $"'{Response.PostalCode}', city '{Response.City}', country '{Response.Country}'");
        return this;
    }

    /// <summary>UI only: the email is shown but cannot be edited.</summary>
    public ViewProfileVerification EmailCannotBeChanged()
    {
        Response.EmailChangeable.ShouldBe(false,
            $"Expected the profile email not to be changeable, but it was (or the channel does not show it: {Response.EmailChangeable})");
        return this;
    }

    /// <summary>UI only: the country select offers exactly these countries, in this order.</summary>
    public ViewProfileVerification CountryChoices(params string[] expectedCountries)
    {
        Response.CountryChoices.ShouldNotBeNull("Expected the profile to show its country choices");
        Response.CountryChoices.ShouldBe(expectedCountries, false,
            $"Expected country choices [{string.Join(", ", expectedCountries)}], but were [{string.Join(", ", Response.CountryChoices)}]");
        return this;
    }

    /// <summary>UI only: the note on how sales tax is charged.</summary>
    public ViewProfileVerification TaxNotice(string expectedNotice)
    {
        Response.TaxNotice.ShouldBe(expectedNotice,
            $"Expected tax notice '{expectedNotice}', but was '{Response.TaxNotice}'");
        return this;
    }

    /// <summary>UI only: the notification shown after saving.</summary>
    public ViewProfileVerification Message(string expectedMessage)
    {
        Response.Message.ShouldBe(expectedMessage,
            $"Expected the message '{expectedMessage}' after saving the profile, but was '{Response.Message}'");
        return this;
    }

    public ViewProfileVerification StreetIsNot(string unexpectedStreet)
    {
        Response.Street.ShouldNotBe(unexpectedStreet,
            $"Expected street not to be '{unexpectedStreet}', but it was");
        return this;
    }

    public ViewProfileVerification PostalCode(string? expectedPostalCode)
    {
        Response.PostalCode.ShouldBe(expectedPostalCode,
            $"Expected postal code to be '{expectedPostalCode}', but was '{Response.PostalCode}'");
        return this;
    }

    public ViewProfileVerification City(string? expectedCity)
    {
        Response.City.ShouldBe(expectedCity,
            $"Expected city to be '{expectedCity}', but was '{Response.City}'");
        return this;
    }

    public ViewProfileVerification Country(string? expectedCountry)
    {
        Response.Country.ShouldBe(expectedCountry,
            $"Expected country to be '{expectedCountry}', but was '{Response.Country}'");
        return this;
    }
}
