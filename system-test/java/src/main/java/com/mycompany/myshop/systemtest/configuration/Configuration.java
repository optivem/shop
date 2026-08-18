package com.mycompany.myshop.systemtest.configuration;

import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Configuration {
    private final String myShopUiBaseUrl;
    private final String myShopApiBaseUrl;
    private final String erpBaseUrl;
    private final String clockBaseUrl;
    private final String taxBaseUrl;
    private final ExternalSystemMode externalSystemMode;
    private final ChannelMode channelMode;
}
