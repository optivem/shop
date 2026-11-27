namespace Driver.Adapter.Shared.Client.Http;

/// <summary>A successful non-JSON response: its Content-Type and raw body.</summary>
public record BinaryResponse(string? ContentType, byte[] Body);
