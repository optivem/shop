using Dsl.Port;
using Optivem.Testing;

namespace Dsl.Core.Scenario.Then;

/// <summary>
/// Runs axe on the page the When step left open, on the same channel, and fails on any serious or critical violation
/// (FT Step 4). A dedicated assertion: scenarios ask for it, it never runs after every step.
/// </summary>
internal static class ThenAccessibility
{
    public static async Task ExpectPageIsAccessibleAsync(UseCaseDsl app, Channel channel)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, channel);
        (await shop.CheckAccessibility().Execute()).ShouldSucceed().NoViolations();
    }
}
