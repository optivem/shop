using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Entities;
using MyCompany.MyShop.Backend.Data;

namespace MyCompany.MyShop.Backend.Core.Services;

/// <summary>
/// The signed-in customer's profile, keyed by the token subject. The request is validated before it gets
/// here (see <see cref="UpdateProfileRequest"/>), so an update always saves.
/// </summary>
public class ProfileService
{
    private readonly AppDbContext _dbContext;

    public ProfileService(AppDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<ViewProfileResponse> ViewProfileAsync(CurrentUser user)
    {
        var profile = await FindAsync(user);
        if (profile == null)
        {
            return new ViewProfileResponse
            {
                FirstName = user.FirstName,
                LastName = user.LastName,
                Email = user.Email
            };
        }

        return ToResponse(profile, user);
    }

    /// <summary>Inserts or replaces the caller's profile with the trimmed values.</summary>
    public async Task<ViewProfileResponse> UpdateProfileAsync(UpdateProfileRequest request, CurrentUser user)
    {
        var profile = await FindAsync(user);
        if (profile == null)
        {
            profile = new CustomerProfile { Owner = user.Subject };
            _dbContext.CustomerProfiles.Add(profile);
        }

        profile.FirstName = request.FirstName!.Trim();
        profile.LastName = request.LastName!.Trim();
        profile.Street = request.Street!.Trim();
        profile.PostalCode = request.PostalCode!.Trim();
        profile.City = request.City!.Trim();
        profile.Country = request.Country!.Trim();
        profile.UpdatedAt = DateTime.UtcNow;

        await _dbContext.SaveChangesAsync();

        return ToResponse(profile, user);
    }

    private Task<CustomerProfile?> FindAsync(CurrentUser user) =>
        _dbContext.CustomerProfiles.FirstOrDefaultAsync(p => p.Owner == user.Subject);

    private static ViewProfileResponse ToResponse(CustomerProfile profile, CurrentUser user) => new()
    {
        FirstName = profile.FirstName,
        LastName = profile.LastName,
        Email = user.Email,
        Street = profile.Street,
        PostalCode = profile.PostalCode,
        City = profile.City,
        Country = profile.Country
    };
}
