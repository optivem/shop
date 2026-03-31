package com.optivem.starter.systemtest.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Configuration {
    private final String shopUiBaseUrl;
    private final String shopApiBaseUrl;
    private final String erpBaseUrl;
    private final String clockBaseUrl;
    private final ExternalSystemMode externalSystemMode;
}
