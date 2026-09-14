package com.cinema.booking.common.exception;

public record FieldErrorDetail (
    String fieldName,
    String message
) {}
