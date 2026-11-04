package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds a URL query string from the parameters that are set; null values are left out. */
final class QueryString {
    private QueryString() {
    }

    static String of(Map<String, String> parameters) {
        var query = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return query.isEmpty() ? "" : "?" + query;
    }
}
