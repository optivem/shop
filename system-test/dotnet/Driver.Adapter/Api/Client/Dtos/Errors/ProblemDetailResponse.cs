using System.Text.Json.Serialization;
using Driver.Adapter.Shared.Client.Http;

namespace Driver.Adapter.Api.Client.Dtos.Errors;

public class ProblemDetailResponse : IResponseMetadataAware
{
    public string? Type { get; set; }
    public string? Title { get; set; }
    public int? Status { get; set; }
    public string? Detail { get; set; }
    public string? Instance { get; set; }
    public string? Timestamp { get; set; }
    public List<FieldError>? Errors { get; set; }

    // From the HTTP response, not the body.
    [JsonIgnore]
    public int? HttpStatus { get; private set; }
    [JsonIgnore]
    public string? RetryAfter { get; private set; }
    [JsonIgnore]
    public string? ContentType { get; private set; }

    public void CaptureResponseMetadata(int httpStatus, string? retryAfter, string? contentType)
    {
        HttpStatus = httpStatus;
        RetryAfter = retryAfter;
        ContentType = contentType;
    }

    public class FieldError
    {
        public string? Field { get; set; }
        public string? Message { get; set; }
        public string? Code { get; set; }
        public object? RejectedValue { get; set; }
    }
}

