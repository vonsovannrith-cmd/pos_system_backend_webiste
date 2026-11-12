package com.example.pos_system.common;

public class PasswordRules {

    // 8–72 characters, includes letters and numbers, ASCII only
    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,72}$";
    public static final String MESSAGE = "The password must be 8–72 characters long and contain letters and numbers (use only Latin characters/symbols).";

    private PasswordRules() {}
}
