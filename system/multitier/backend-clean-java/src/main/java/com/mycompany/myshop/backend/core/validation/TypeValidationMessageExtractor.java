package com.mycompany.myshop.backend.core.validation;

import java.util.HashMap;
import java.util.Map;

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
