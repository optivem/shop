using System.Text.Json;
using System.Text.Json.Serialization;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// The storefront's notification banner, rendered by the layout with the same markup as the order pages' own
/// notifications (<c>[role='alert'].notification.success|error</c>, <c>data-notification-id</c>).
/// </summary>
public sealed record Notice(string Type, string Message, IReadOnlyList<string> FieldErrors, string Id)
{
    /// <summary>The session key of a notice shown on the page a redirect lands on (post-redirect-get).</summary>
    internal const string FlashSessionKey = "myshop.notice";

    /// <summary>The request item of a notice the current page shows itself.</summary>
    internal const string PageItemKey = "myshop.notice";

    private static long _counter;

    [JsonIgnore]
    public bool IsError => Type == "error";

    public static Notice Success(string message) => new("success", message, [], NextId());

    public static Notice Error(string message, IEnumerable<string>? fieldErrors = null) =>
        new("error", message, (fieldErrors ?? []).ToList(), NextId());

    /// <summary>Shows the notice on the page the next request renders (after a redirect).</summary>
    public static void Flash(ISession session, Notice notice) =>
        session.SetString(FlashSessionKey, JsonSerializer.Serialize(notice));

    /// <summary>Shows the notice on the page this request renders.</summary>
    public static void Show(HttpContext context, Notice notice) => context.Items[PageItemKey] = notice;

    /// <summary>The notice for this page, if any; a flashed one is shown once.</summary>
    public static Notice? Take(HttpContext context)
    {
        if (context.Items.TryGetValue(PageItemKey, out var shown) && shown is Notice notice)
        {
            return notice;
        }

        var json = context.Session.GetString(FlashSessionKey);
        if (json == null)
        {
            return null;
        }

        context.Session.Remove(FlashSessionKey);
        return JsonSerializer.Deserialize<Notice>(json);
    }

    /// <summary>Unique per notice, so a test can tell a new notification from the previous one.</summary>
    private static string NextId() => "store-" + Interlocked.Increment(ref _counter);
}
