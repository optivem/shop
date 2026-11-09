using ArchUnitNET.Domain;
using ArchUnitNET.Domain.Extensions;
using ArchUnitNET.Fluent;
using ArchUnitNET.Fluent.Conditions;
using ArchUnitNET.Loader;
using ArchUnitNET.xUnit;
using Xunit;
using static ArchUnitNET.Fluent.ArchRuleDefinition;

namespace SystemTests.Latest.ArchitectureTests;

/// <summary>
/// POC parity with Java's ArchitectureRulesTest: which DSL/driver structural rules can be
/// enforced mechanically? Four representative rules (A1/A2/A7/A10) via ArchUnitNET.
/// </summary>
public class ArchitectureRulesTest
{
    private static readonly Architecture TestKit = new ArchLoader()
        .LoadAssemblies(
            typeof(Driver.Port.IMyShopDriver).Assembly,
            typeof(Dsl.Core.Shared.VoidVerification).Assembly,
            typeof(Dsl.Core.UseCase.UseCases.ViewOrderVerification).Assembly)
        .Build();

    // A1 — request DTOs in the driver port expose only string members, or a nested driver-port DTO that
    // itself exposes only string members (e.g. PlaceOrderRequest.ShippingAddress).
    [Fact]
    [Trait("Category", "Architecture")]
    public void RequestDtosDeclareOnlyStringMembers()
    {
        static bool IsString(IType type) => type.Name == "String";

        static bool IsStringOnlyDto(IType type) =>
            type is Class nested
            && nested.Namespace.FullName == "Driver.Port.Dtos"
            && nested.GetPropertyMembers().All(p => IsString(p.Type));

        IArchRule rule = Classes().That()
            .ResideInNamespace("Driver.Port.Dtos").And()
            .HaveNameEndingWith("Request")
            .Should().FollowCustomCondition(cls =>
            {
                var nonString = cls.GetPropertyMembers()
                    .Where(p => !IsString(p.Type) && !IsStringOnlyDto(p.Type))
                    .ToList();
                var pass = nonString.Count == 0;
                var failure = pass
                    ? ""
                    : $"{cls.FullName} declares non-string members: "
                      + string.Join(", ", nonString.Select(p => $"{p.Name}:{p.Type.Name}"));
                return new ConditionResult(cls, pass, failure);
            }, "Request DTOs in Driver.Port.Dtos must declare only string members");

        rule.Check(TestKit);
    }

    // A2 — verification public methods are fluent (own type) or terminal (void); never getters.
    [Fact]
    [Trait("Category", "Architecture")]
    public void VerificationMethodsAreFluentOrVoid()
    {
        IArchRule rule = Classes().That()
            .ResideInNamespaceMatching("Dsl\\.Core").And()
            .HaveNameEndingWith("Verification")
            .Should().FollowCustomCondition(cls =>
            {
                var violations = cls.GetMethodMembers()
                    .Where(m => m.Visibility == Visibility.Public)
                    .Where(m => m.ReturnType.FullName != cls.FullName && m.ReturnType.Name != "Void")
                    .ToList();
                var pass = violations.Count == 0;
                var failure = pass
                    ? ""
                    : $"{cls.FullName} has non-fluent public methods: "
                      + string.Join(", ", violations.Select(m => $"{m.Name}->{m.ReturnType.Name}"));
                return new ConditionResult(cls, pass, failure);
            }, "Verification public methods must return their own type (fluent) or void (terminal) — no getters");

        rule.Check(TestKit);
    }

    // A7 — DSL core declares no own *Request/*Response; it reuses Driver.Port.Dtos.
    [Fact]
    [Trait("Category", "Architecture")]
    public void DslCoreDeclaresNoOwnRequestOrResponseDtos()
    {
        IArchRule rule = Classes().That()
            .ResideInNamespaceMatching("Dsl\\.Core")
            .Should().NotHaveNameEndingWith("Request")
            .AndShould().NotHaveNameEndingWith("Response");

        rule.Check(TestKit);
    }

    // A10 — every IMyShopDriver method takes a single *Request and returns Result<*Response, …> (strict).
    [Fact]
    [Trait("Category", "Architecture")]
    public void EveryDriverMethodTakesRequestAndReturnsResponse()
    {
        IArchRule rule = Interfaces().That()
            .HaveName("IMyShopDriver")
            .Should().FollowCustomCondition(driver =>
            {
                var problems = new List<string>();
                foreach (var method in driver.GetMethodMembers())
                {
                    var parameters = method.Parameters.ToList();
                    if (parameters.Count != 1 || !parameters[0].Name.EndsWith("Request"))
                    {
                        problems.Add(
                            $"{method.Name} must take exactly one *Request parameter "
                            + $"(was [{string.Join(", ", parameters.Select(p => p.Name))}])");
                    }

                    var returnsResponse = method.Dependencies.Any(d => d.Target.Name.EndsWith("Response"));
                    if (!returnsResponse)
                    {
                        problems.Add($"{method.Name} must return Result<*Response, …>");
                    }
                }

                var pass = problems.Count == 0;
                return new ConditionResult(driver, pass, string.Join("; ", problems));
            }, "Every IMyShopDriver method must take a single *Request and return Result<*Response, …>");

        rule.Check(TestKit);
    }
}
