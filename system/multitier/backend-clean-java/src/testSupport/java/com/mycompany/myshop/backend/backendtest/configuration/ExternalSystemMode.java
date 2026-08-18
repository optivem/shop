package com.mycompany.myshop.backend.backendtest.configuration;

public enum ExternalSystemMode {

    STUB,

    REAL;

    public String propertyValue() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
