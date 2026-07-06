package com.example.agent;

public class AgentException extends Exception {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
