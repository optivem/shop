package com.mycompany.myshop.core.validation;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TypeValidationMessageExtractor {

    private TypeValidationMessageExtractor() {
    }

    public static Map<String, String> extractFieldMessages(Class<?> clazz) {
        Map<String, String> fieldMessages = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(TypeValidationMessage.class)) {
                TypeValidationMessage annotation = field.getAnnotation(TypeValidationMessage.class);
                String fieldName = field.getName().toLowerCase();
                fieldMessages.put(fieldName, annotation.value());
            }
        }

        return fieldMessages;
    }
}
