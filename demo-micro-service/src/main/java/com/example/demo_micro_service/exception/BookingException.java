package com.example.demo_micro_service.exception;

public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
