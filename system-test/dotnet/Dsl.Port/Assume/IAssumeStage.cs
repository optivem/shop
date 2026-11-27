namespace Dsl.Port.Assume;

using Dsl.Port.Assume.Steps;

public interface IAssumeStage
{
    IAssumeRunning MyShop();

    IAssumeRunning Erp();

    IAssumeRunning Tax();

    IAssumeRunning Clock();

    IAssumeRunning IdentityProvider();
}
