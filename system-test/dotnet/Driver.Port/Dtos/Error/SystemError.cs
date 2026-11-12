namespace Driver.Port.Dtos.Error;

public class SystemError
{
    public string Message { get; private set; }
    public IReadOnlyList<FieldError>? Fields { get; private set; }

    /// <summary>The HTTP status, Retry-After header and Content-Type of the failed call; null where the channel has no HTTP response (UI).</summary>
    public int? HttpStatus { get; private set; }
    public string? RetryAfter { get; private set; }
    public string? ContentType { get; private set; }

    public SystemError WithResponseMetadata(int? httpStatus, string? retryAfter, string? contentType)
    {
        HttpStatus = httpStatus;
        RetryAfter = retryAfter;
        ContentType = contentType;
        return this;
    }

    private SystemError(string message, IReadOnlyList<FieldError>? fields = null)
    {
        Message = message;
        Fields = fields;
    }

    public static SystemError Of(string message)
    {
        return new SystemError(message);
    }

    public static SystemError Of(string message, params FieldError[] fieldErrors)
    {
        return new SystemError(message, fieldErrors.ToList().AsReadOnly());
    }

    public static SystemError Of(string message, IReadOnlyList<FieldError> fieldErrors)
    {
        return new SystemError(message, fieldErrors);
    }

    public override string ToString()
    {
        var sb = new System.Text.StringBuilder();
        sb.Append($"SystemError{{message='{Message}'");

        if (Fields != null && Fields.Any())
        {
            sb.Append(", fieldErrors=[");
            for (int i = 0; i < Fields.Count; i++)
            {
                if (i > 0) sb.Append(", ");
                sb.Append(Fields[i]);
            }
            sb.Append(']');
        }

        sb.Append('}');
        return sb.ToString();
    }

    public class FieldError
    {
        public string Field { get; }
        public string Message { get; }
        public string? Code { get; }

        public FieldError(string field, string message, string? code = null)
        {
            Field = field;
            Message = message;
            Code = code;
        }

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            sb.Append($"FieldError{{field='{Field}'");
            sb.Append($", message='{Message}'");
            if (!string.IsNullOrEmpty(Code))
            {
                sb.Append($", code='{Code}'");
            }
            sb.Append('}');
            return sb.ToString();
        }
    }
}

