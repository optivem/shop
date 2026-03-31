package com.optivem.starter.monolith.models;

public class GreetingResponse {
    private String greeting;
    private String message;
    private String localTime;
    private int estimatedReadingTimeSeconds;

    public GreetingResponse() {
    }

    public GreetingResponse(String greeting, String message, String localTime,
                            int estimatedReadingTimeSeconds) {
        this.greeting = greeting;
        this.message = message;
        this.localTime = localTime;
        this.estimatedReadingTimeSeconds = estimatedReadingTimeSeconds;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLocalTime() {
        return localTime;
    }

    public void setLocalTime(String localTime) {
        this.localTime = localTime;
    }

    public int getEstimatedReadingTimeSeconds() {
        return estimatedReadingTimeSeconds;
    }

    public void setEstimatedReadingTimeSeconds(int estimatedReadingTimeSeconds) {
        this.estimatedReadingTimeSeconds = estimatedReadingTimeSeconds;
    }
}
