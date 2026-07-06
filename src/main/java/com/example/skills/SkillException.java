package com.example.skills;

public class SkillException extends Exception {

    public SkillException(String message) {
        super(message);
    }

    public SkillException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
