package com.mycompany.myshop.controllers.web.storefront;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The storefront's notification banner, rendered by the layout with the same markup as the order pages' own
 * notifications ({@code [role='alert'].notification.success|error}, {@code data-notification-id}).
 */
public record Notice(String type, String message, List<String> fieldErrors, String id) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The model / flash attribute the layout reads. */
    public static final String ATTRIBUTE = "notice";

    private static final AtomicLong COUNTER = new AtomicLong();

    public static Notice success(String message) {
        return new Notice("success", message, List.of(), nextId());
    }

    public static Notice error(String message) {
        return error(message, List.of());
    }

    public static Notice error(String message, List<String> fieldErrors) {
        return new Notice("error", message, List.copyOf(fieldErrors), nextId());
    }

    public boolean isError() {
        return "error".equals(type);
    }

    /** Unique per notice, so a test can tell a new notification from the previous one. */
    private static String nextId() {
        return "store-" + COUNTER.incrementAndGet();
    }
}
