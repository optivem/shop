namespace Driver.Port.Dtos;

public class BrowseOrderHistoryRequest
{
    public string? OrderNumber { get; set; }
    public string? Page { get; set; }
    public string? Size { get; set; }

    /// <summary>The username whose orders an admin wants to see; ignored for customers.</summary>
    public string? Customer { get; set; }
}
