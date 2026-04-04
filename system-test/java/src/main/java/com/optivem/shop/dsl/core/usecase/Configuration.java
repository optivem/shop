package com.optivem.shop.dsl.core.usecase;

import com.optivem.shop.dsl.port.ExternalSystemMode;
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
