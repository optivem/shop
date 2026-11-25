using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Driver.Port.Dtos;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>A visitor signs up (UC-013) with this account and shipping country (UI only).</summary>
public class SignUp : BaseMyShopUseCase<VoidValue, VoidVerification>
{
    private readonly SignUpRequest _request = new();

    public SignUp(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public SignUp Account(string username, string password)
    {
        _request.Username = username;
        _request.Password = password;
        return this;
    }

    public SignUp Email(string email)
    {
        _request.Email = email;
        return this;
    }

    public SignUp Name(string firstName, string lastName)
    {
        _request.FirstName = firstName;
        _request.LastName = lastName;
        return this;
    }

    public SignUp Country(string country)
    {
        _request.Country = country;
        return this;
    }

    public override async Task<MyShopUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var result = await _driver.SignUpAsync(_request).MapVoidAsync();

        return new MyShopUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
