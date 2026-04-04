using Dsl.Port;
using Microsoft.CodeAnalysis.CSharp.Syntax;
using Dsl.Core.Shared;

namespace Dsl.Core;

public class Configuration
{
    private readonly string shopUiBaseUrl;
    private readonly string shopApiBaseUrl;
    private readonly string erpBaseUrl;
    private readonly string clockBaseUrl;
    private readonly ExternalSystemMode externalSystemMode;
    private readonly ChannelMode channelMode;
    private readonly string? staticChannel;

    public Configuration(string shopUiBaseUrl, string shopApiBaseUrl, string erpBaseUrl, string clockBaseUrl,
        ExternalSystemMode externalSystemMode, ChannelMode channelMode = ChannelMode.Dynamic, string? staticChannel = null)
    {
        this.shopUiBaseUrl = shopUiBaseUrl;
        this.shopApiBaseUrl = shopApiBaseUrl;
        this.erpBaseUrl = erpBaseUrl;
        this.clockBaseUrl = clockBaseUrl;
        this.externalSystemMode = externalSystemMode;
        this.channelMode = channelMode;
        this.staticChannel = staticChannel;
    }

    public string ShopUiBaseUrl => shopUiBaseUrl;
    public string ShopApiBaseUrl => shopApiBaseUrl;
    public string ErpBaseUrl => erpBaseUrl;
    public string ClockBaseUrl => clockBaseUrl;
    public ExternalSystemMode ExternalSystemMode => externalSystemMode;
    public ChannelMode ChannelMode => channelMode;
    public string? StaticChannel => staticChannel;
}



