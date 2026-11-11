package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;

import java.util.List;

import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.failure;
import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.success;

public abstract class BasePage {
    private static final String NOTIFICATION_SELECTOR = "[role='alert']";
    private static final String NOTIFICATION_SUCCESS_SELECTOR = "[role='alert'].notification.success";
    private static final String NOTIFICATION_ERROR_SELECTOR = "[role='alert'].notification.error";
    private static final String NOTIFICATION_ERROR_MESSAGE_SELECTOR = "[role='alert'].notification.error .error-message";
    private static final String NOTIFICATION_ERROR_FIELD_SELECTOR = "[role='alert'].notification.error .field-error";
    private static final String NOTIFICATION_ID_ATTRIBUTE = "data-notification-id";
    private static final String CART_ITEM_COUNT_SELECTOR = "[aria-label='Cart Item Count']";

    private static final String UNRECOGNIZED_NOTIFICATION_ERROR_MESSAGE = "Notification type is not recognized";

    protected final PageClient pageClient;

    private String lastNotificationId = null;

    protected BasePage(PageClient pageClient) {
        this.pageClient = pageClient;
    }

    public Result<String, SystemError> getResult() {
        var notificationId = waitForNewNotification();
        lastNotificationId = notificationId;

        var isSuccess = isSuccessNotification(notificationId);

        if (isSuccess) {
            var successMessage = readSuccessNotification(notificationId);
            return success(successMessage);
        }

        var generalMessage = readGeneralErrorMessage(notificationId);
        var fieldErrorTexts = readFieldErrors(notificationId);

        if (fieldErrorTexts.isEmpty()) {
            return failure(generalMessage);
        }

        var fieldErrors = fieldErrorTexts.stream()
            .map(this::parseFieldError)
                .toList();

        var error = SystemError.builder()
                .message(generalMessage)
                .fields(fieldErrors)
                .build();

        return failure(error);
    }

    private String waitForNewNotification() {
        var selector = lastNotificationId == null
                ? NOTIFICATION_SELECTOR + "[" + NOTIFICATION_ID_ATTRIBUTE + "]"
                : NOTIFICATION_SELECTOR + "[" + NOTIFICATION_ID_ATTRIBUTE + "]:not([" + NOTIFICATION_ID_ATTRIBUTE + "='" + lastNotificationId + "'])";

        pageClient.waitForVisible(selector);

        var notificationId = pageClient.readAttribute(selector, NOTIFICATION_ID_ATTRIBUTE);

        if(notificationId == null) {
            throw new IllegalStateException("Notification does not have an ID");
        }

        return notificationId;
    }

    private boolean isSuccessNotification(String notificationId) {
        var successSelector = withNotificationId(NOTIFICATION_SUCCESS_SELECTOR, notificationId);
        var isSuccess = pageClient.isVisible(successSelector);

        if(isSuccess) {
            return true;
        }

        var errorSelector = withNotificationId(NOTIFICATION_ERROR_SELECTOR, notificationId);
        var isError = pageClient.isVisible(errorSelector);

        if(isError) {
            return false;
        }

        throw new IllegalStateException(UNRECOGNIZED_NOTIFICATION_ERROR_MESSAGE);
    }

    private String readSuccessNotification(String notificationId) {
        var selector = withNotificationId(NOTIFICATION_SUCCESS_SELECTOR, notificationId);
        return pageClient.readTextContent(selector);
    }

    private String readGeneralErrorMessage(String notificationId) {
        var selector = withNotificationId(NOTIFICATION_ERROR_MESSAGE_SELECTOR, notificationId);
        return pageClient.readTextContent(selector);
    }

    private List<String> readFieldErrors(String notificationId) {
        var selector = withNotificationId(NOTIFICATION_ERROR_FIELD_SELECTOR, notificationId);
        if (!pageClient.isVisible(selector)) {
            return List.of();
        }
        return pageClient.readAllTextContents(selector);
    }

    private SystemError.FieldError parseFieldError(String text) {
        var parts = text.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid field error format: " + text);
        }

        return new SystemError.FieldError(
                parts[0].trim(),
                parts[1].trim()
        );
    }

    /** The notification showing now, without waiting for one: the success text or the error's message; else null. */
    protected String readShownNotification() {
        var success = pageClient.readAllTextContentsNoWait(NOTIFICATION_SUCCESS_SELECTOR);
        if (!success.isEmpty()) {
            return success.get(0).trim();
        }
        var error = pageClient.readAllTextContentsNoWait(NOTIFICATION_ERROR_MESSAGE_SELECTOR);
        return error.isEmpty() ? null : error.get(0).trim();
    }

    /** The header's cart badge; 0 when it is not shown. */
    public Integer readCartItemCount() {
        if (pageClient.isHidden(CART_ITEM_COUNT_SELECTOR)) {
            return 0;
        }
        return Integer.parseInt(pageClient.readTextContent(CART_ITEM_COUNT_SELECTOR).trim());
    }

    private static String withNotificationId(String selector, String notificationId) {
        var idAttribute = "[" + NOTIFICATION_ID_ATTRIBUTE + "='" + notificationId + "']";
        return selector.replace(NOTIFICATION_SELECTOR, NOTIFICATION_SELECTOR + idAttribute);
    }
}
