namespace Driver.Port.Dtos;

public class BrowseOrderHistoryRequest
{
    /// <summary><see cref="FollowUp"/>: after browsing, set the customer filter to All customers and clear the order number (UI only).</summary>
    public const string ResetFilters = "reset-filters";

    public string? OrderNumber { get; set; }
    public string? Page { get; set; }
    public string? Size { get; set; }

    /// <summary>The username whose orders an admin wants to see; ignored for customers.</summary>
    public string? Customer { get; set; }

    /// <summary>What the user does after browsing: <see cref="ResetFilters"/>, or nothing (unset). UI only.</summary>
    public string? FollowUp { get; set; }
}
