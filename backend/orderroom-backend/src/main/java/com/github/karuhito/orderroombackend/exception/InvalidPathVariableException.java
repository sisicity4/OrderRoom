package com.github.karuhito.orderroombackend.exception;

public class InvalidPathVariableException extends RuntimeException {
    private final String fieldName;
    private final String invalidValue;

    public InvalidPathVariableException(String fieldName, String invalidValue) {
        super(fieldName + "=" + invalidValue);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

}