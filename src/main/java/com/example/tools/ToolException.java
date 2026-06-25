package com.example.tools;

public class ToolException extends Exception {

    public ToolException(String message) {
        super(message);
    }

    public ToolException(String message, Exception e) {
        super(message, e);
    }
}
