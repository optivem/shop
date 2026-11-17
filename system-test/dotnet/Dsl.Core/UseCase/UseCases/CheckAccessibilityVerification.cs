using Dsl.Core.Shared;
using Driver.Port.Dtos;
using Shouldly;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>Verifies the accessibility check of the page that is open.</summary>
public class CheckAccessibilityVerification : ResponseVerification<CheckAccessibilityResponse>
{
    public CheckAccessibilityVerification(CheckAccessibilityResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    /// <summary>axe found no serious or critical violation.</summary>
    public CheckAccessibilityVerification NoViolations()
    {
        Response.Violations.ShouldBeEmpty(
            $"Expected no serious or critical axe violations on the page, but found: {string.Join("; ", Response.Violations)}");
        return this;
    }
}
