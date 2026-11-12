namespace Driver.Adapter.Shared.Client.Http;

/// <summary>
/// An error body that also records the HTTP response it came from: the status code, the
/// <c>Retry-After</c> header and the <c>Content-Type</c>, none of which are in the body itself.
/// </summary>
public interface IResponseMetadataAware
{
    void CaptureResponseMetadata(int httpStatus, string? retryAfter, string? contentType);
}
