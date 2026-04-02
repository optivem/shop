using Driver.Adapter.Shared.Client.Playwright;

using Common;

using Driver.Port.Shop;

using Driver.Port.Shop.Dtos.Error;



namespace Driver.Adapter.Shop.Ui.Client.Pages;



public abstract class BasePage

{

    private const string NotificationSelector = "[role='alert'][data-notification-id]";

    private const string NotificationSuccessSelector = "[role='alert'].notification.success";

    private const string NotificationErrorSelector = "[role='alert'].notification.error";

    private const string NotificationErrorMessageSelector = "[role='alert'].notification.error .error-message";

    private const string NotificationErrorFieldSelector = "[role='alert'].notification.error .field-error";

    private const string NotificationIdAttribute = "data-notification-id";

    private const string UnrecognizedNotificationErrorMessage = "Notification type is not recognized";



    protected readonly PageClient PageClient;

    

    private string? _lastNotificationId;



    protected BasePage(PageClient pageClient)

    {

        PageClient = pageClient;

    }



    public async Task<Result<string, SystemError>> GetResultAsync()

    {

        var notificationId = await WaitForNewNotificationAsync();

        _lastNotificationId = notificationId;



        var isSuccess = await IsSuccessNotificationAsync(notificationId);



        if (isSuccess)

        {

            var successMessage = await ReadSuccessNotificationAsync(notificationId);

            return SystemResults.Success(successMessage);

        }



        var generalMessage = await ReadGeneralErrorMessageAsync(notificationId);

        var fieldErrorTexts = await ReadFieldErrorsAsync(notificationId);



        if (fieldErrorTexts.Count == 0)

        {

            return SystemResults.Failure<string>(generalMessage);

        }



        var fieldErrors = fieldErrorTexts.Select(ParseFieldError).ToList();



        var error = SystemError.Of(generalMessage, fieldErrors);



        return SystemResults.Failure<string>(error);

    }



    private async Task<string> WaitForNewNotificationAsync()

    {

        var selector = _lastNotificationId == null

            ? NotificationSelector

            : $"{NotificationSelector}:not([{NotificationIdAttribute}='{_lastNotificationId}'])";



        await PageClient.WaitForVisibleAsync(selector);



        var notificationId = await PageClient.ReadAttributeAsync(selector, NotificationIdAttribute);

        

        if(notificationId == null) 

        {

            throw new InvalidOperationException($"Notification element does not have {NotificationIdAttribute} attribute");

        }



        return notificationId;

    }



    private async Task<bool> IsSuccessNotificationAsync(string notificationId)

    {

        var successSelector = WithNotificationId(NotificationSuccessSelector, notificationId);

        var isSuccess = await PageClient.IsVisibleAsync(successSelector);



        if (isSuccess)

        {

            return true;

        }



        var errorSelector = WithNotificationId(NotificationErrorSelector, notificationId);

        var isError = await PageClient.IsVisibleAsync(errorSelector);



        if (isError)

        {

            return false;

        }



        throw new InvalidOperationException(UnrecognizedNotificationErrorMessage);

    }



    private async Task<string> ReadSuccessNotificationAsync(string notificationId)

    {

        var selector = WithNotificationId(NotificationSuccessSelector, notificationId);

        return await PageClient.ReadTextContentAsync(selector);

    }



    private async Task<string> ReadGeneralErrorMessageAsync(string notificationId)

    {

        var selector = WithNotificationId(NotificationErrorMessageSelector, notificationId);

        return await PageClient.ReadTextContentAsync(selector);

    }



    private async Task<List<string>> ReadFieldErrorsAsync(string notificationId)

    {

        var selector = WithNotificationId(NotificationErrorFieldSelector, notificationId);

        

        if (!await PageClient.IsVisibleAsync(selector))

        {

            return new List<string>();

        }

        

        return await PageClient.ReadAllTextContentsAsync(selector);

    }



    private static SystemError.FieldError ParseFieldError(string text)

    {

        var parts = text.Split(':', 2);



        if (parts.Length != 2)

        {

            throw new InvalidOperationException($"Invalid field error format: {text}");

        }



        return new SystemError.FieldError(parts[0].Trim(), parts[1].Trim());

    }



    private static string WithNotificationId(string selector, string notificationId)

    {

        var idAttribute = $"[{NotificationIdAttribute}='{notificationId}']";

        return selector.Replace(NotificationSelector, NotificationSelector + idAttribute);

    }

}







