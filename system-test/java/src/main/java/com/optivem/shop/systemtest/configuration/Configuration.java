package com.optivem.shop.systemtest.configuration;

import com.optivem.shop.testkit.dsl.port.ChannelMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Configuration {
    private final String shopUiBaseUrl;
    private final String shopApiBaseUrl;
    private final String erpBaseUrl;
    private final String clockBaseUrl;
    private final String taxBaseUrl;
    private final ExternalSystemMode externalSystemMode;
    private final ChannelMode channelMode;
}
