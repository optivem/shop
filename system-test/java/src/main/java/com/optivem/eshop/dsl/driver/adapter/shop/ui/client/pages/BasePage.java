package com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages;

import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;
import com.optivem.eshop.dsl.driver.adapter.shared.client.playwright.PageClient;

import java.util.List;

import static com.optivem.eshop.dsl.core.usecase.shop.commons.SystemResults.failure;
import static com.optivem.eshop.dsl.core.usecase.shop.commons.SystemResults.success;

public abstract class BasePage {
    private static final String NOTIFICATION_SELECTOR = "[role='alert']";
    private static final String NOTIFICATION_SUCCESS_SELECTOR = "[role='alert'].notification.success";
    private static final String NOTIFICATION_ERROR_SELECTOR = "[role='alert'].notification.error";
    private static final String NOTIFICATION_ERROR_MESSAGE_SELECTOR = "[role='alert'].notification.error .error-message";
    private static final String NOTIFICATION_ERROR_FIELD_SELECTOR = "[role='alert'].notification.error .field-error";
    private static final String NOTIFICATION_ID_ATTRIBUTE = "data-notification-id";

    private static final String UNRECOGNIZED_NOTIFICATION_ERROR_MESSAGE = "Notification type is not recognized";

    protected final PageClient pageClient;

    private String lastNotificationId = null;

    protected BasePage(PageClient pageClient) {
        this.pageClient = pageClient;
    }

    public Result<String, ErrorResponse> getResult() {
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

        var error = ErrorResponse.builder()
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

    private ErrorResponse.FieldError parseFieldError(String text) {
        var parts = text.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid field error format: " + text);
        }

        return new ErrorResponse.FieldError(
                parts[0].trim(),
                parts[1].trim()
        );
    }

    private static String withNotificationId(String selector, String notificationId) {
        var idAttribute = "[" + NOTIFICATION_ID_ATTRIBUTE + "='" + notificationId + "']";
        return selector.replace(NOTIFICATION_SELECTOR, NOTIFICATION_SELECTOR + idAttribute);
    }
}
