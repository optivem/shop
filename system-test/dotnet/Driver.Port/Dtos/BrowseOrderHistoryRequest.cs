namespace Driver.Port.Dtos;

public class BrowseOrderHistoryRequest
{
    public string? OrderNumber { get; set; }
    public string? Page { get; set; }
    public string? Size { get; set; }
}
