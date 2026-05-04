package com.jobportal.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ApiResponse<T> implements Serializable {
    private Boolean success;
    private String message;
    private Integer responseCode;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .responseCode(200)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .responseCode(200)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .responseCode(400)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, Integer responseCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .responseCode(responseCode)
                .build();
    }
}