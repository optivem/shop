namespace MyCompany.MyShop.Monolith.Core.Exceptions;

/// <summary>
/// The ERP could not answer: it is unreachable, timed out, or returned an error status.
/// A product the ERP reports as missing (404) is not this; that is a validation failure.
/// </summary>
public class ErpUnavailableException : System.Exception
{
    public ErpUnavailableException(string message) : base(message)
    {
    }

    public ErpUnavailableException(string message, System.Exception innerException) : base(message, innerException)
    {
    }
}
