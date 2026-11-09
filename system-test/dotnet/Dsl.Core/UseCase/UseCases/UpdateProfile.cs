using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>Saves the acting user's profile. Profile countries are literals: the profile accepts only US, DE or GB.</summary>
public class UpdateProfile : BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification>
{
    private string? _firstName;
    private string? _lastName;
    private string? _street;
    private string? _postalCode;
    private string? _city;
    private string? _country;

    public UpdateProfile(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public UpdateProfile FirstName(string? firstName)
    {
        _firstName = firstName;
        return this;
    }

    public UpdateProfile LastName(string? lastName)
    {
        _lastName = lastName;
        return this;
    }

    public UpdateProfile Street(string? street)
    {
        _street = street;
        return this;
    }

    public UpdateProfile PostalCode(string? postalCode)
    {
        _postalCode = postalCode;
        return this;
    }

    public UpdateProfile City(string? city)
    {
        _city = city;
        return this;
    }

    public UpdateProfile Country(string? country)
    {
        _country = country;
        return this;
    }

    public override async Task<MyShopUseCaseResult<ViewProfileResponse, ViewProfileVerification>> Execute()
    {
        var request = new UpdateProfileRequest
        {
            FirstName = _firstName,
            LastName = _lastName,
            Street = _street,
            PostalCode = _postalCode,
            City = _city,
            Country = _country,
        };

        var result = await _driver.UpdateProfileAsync(request);

        return new MyShopUseCaseResult<ViewProfileResponse, ViewProfileVerification>(
            result,
            _context,
            (response, ctx) => new ViewProfileVerification(response, ctx));
    }
}
