using Microsoft.AspNetCore.Html;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// The few Lucide icons (ISC licence, https://lucide.dev) the Market storefront uses, as inline SVG: the same
/// icons frontend-react renders with lucide-react. They are decorative (<c>aria-hidden</c>) and carry no text,
/// so they never change the text content the UI drivers read.
/// </summary>
public static class Icons
{
    private static readonly Dictionary<string, string> Shapes = new()
    {
        ["truck"] = "<path d=\"M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2\"/><path d=\"M15 18H9\"/><path d=\"M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14\"/><circle cx=\"17\" cy=\"18\" r=\"2\"/><circle cx=\"7\" cy=\"18\" r=\"2\"/>",
        ["shopping-cart"] = "<path d=\"m2.05 2.05 1.099-.028a1 1 0 0 1 1.008.815l2.69 14.347A1 1 0 0 0 7.83 18H18\"/><path d=\"M4.563 5h16.435a1 1 0 0 1 .981 1.204l-1.026 6.226A2 2 0 0 1 18.962 14H6.25\"/><circle cx=\"18\" cy=\"20\" r=\"2\"/><circle cx=\"8\" cy=\"20\" r=\"2\"/>",
        ["arrow-right"] = "<path d=\"M5 12h14\"/><path d=\"m12 5 7 7-7 7\"/>",
        ["arrow-left"] = "<path d=\"m12 19-7-7 7-7\"/><path d=\"M19 12H5\"/>",
        ["sliders-horizontal"] = "<path d=\"M10 5H3\"/><path d=\"M12 19H3\"/><path d=\"M14 3v4\"/><path d=\"M16 17v4\"/><path d=\"M21 12h-9\"/><path d=\"M21 19h-5\"/><path d=\"M21 5h-7\"/><path d=\"M8 10v4\"/><path d=\"M8 12H3\"/>",
        ["minus"] = "<path d=\"M5 12h14\"/>",
        ["plus"] = "<path d=\"M5 12h14\"/><path d=\"M12 5v14\"/>",
        ["check"] = "<path d=\"M20 6 9 17l-5-5\"/>",
        ["trash"] = "<path d=\"M10 11v6\"/><path d=\"M14 11v6\"/><path d=\"M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6\"/><path d=\"M3 6h18\"/><path d=\"M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2\"/>",
        ["refresh-cw"] = "<path d=\"M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8\"/><path d=\"M21 3v5h-5\"/><path d=\"M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16\"/><path d=\"M8 16H3v5\"/>",
        ["chevron-right"] = "<path d=\"m9 18 6-6-6-6\"/>",
        ["circle-check"] = "<circle cx=\"12\" cy=\"12\" r=\"10\"/><path d=\"m16 9-5.5 5.5L8 12\"/>",
    };

    /// <summary>The named icon as an aria-hidden inline SVG of the given size, with an optional extra CSS class.</summary>
    public static IHtmlContent Svg(string name, int size = 16, string? cssClass = null, double strokeWidth = 2)
    {
        var classAttribute = cssClass == null ? string.Empty : $" class=\"{cssClass}\"";
        var stroke = strokeWidth.ToString(System.Globalization.CultureInfo.InvariantCulture);
        return new HtmlString(
            $"<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"{size}\" height=\"{size}\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"{stroke}\" stroke-linecap=\"round\" stroke-linejoin=\"round\"{classAttribute} aria-hidden=\"true\" focusable=\"false\">{Shapes[name]}</svg>");
    }
}
