package com.mycompany.myshop.backend.presentation.exception;

import com.mycompany.myshop.backend.usecases.TypeValidationMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception-handler plumbing: reads the {@link TypeValidationMessage} hints off a request DTO so a
 * Jackson type-mismatch can be reported as a field validation error rather than a parse failure.
 * Lives in presentation because the exception handler is its only caller; the annotation itself
 * lives in {@code usecases} alongside the request DTOs it annotates.
 */
public class TypeValidationMessageExtractor {

    private TypeValidationMessageExtractor() {
    }

    public static Map<String, String> extractFieldMessages(Class<?> clazz) {
        var fieldMessages = new HashMap<String, String>();

        for (var field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(TypeValidationMessage.class)) {
                var annotation = field.getAnnotation(TypeValidationMessage.class);
                var fieldName = field.getName().toLowerCase();
                fieldMessages.put(fieldName, annotation.value());
            }
        }

        return fieldMessages;
    }
}
