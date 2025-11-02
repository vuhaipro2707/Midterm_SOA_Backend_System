package com.example.mail_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> GenericResponse<T> success(String message, T data) {
        return new GenericResponse<>(true, message, data);
    }

    public static <T> GenericResponse<T> success(String message) {
        return new GenericResponse<>(true, message, null);
    }

    public static <T> GenericResponse<T> failure(String message) {
        return new GenericResponse<>(false, message, null);
    }
}