package com.jobportal.v1.util;

import com.jobportal.v1.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for standardizing API responses across all controllers.
 * This reduces code duplication and ensures consistent response format.
 */
public final class ResponseUtil {

    private ResponseUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Create success response with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * Create success response without data (returns Void type)
     * Use this when method returns ResponseEntity<ApiResponse<Void>>
     */
    public static ResponseEntity<ApiResponse<Void>> ok(String message) {
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    /**
     * Create success response with String data
     * Use this when method returns ResponseEntity<ApiResponse<String>>
     */
    public static ResponseEntity<ApiResponse<String>> okString(String message, String data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * Create success response with String message only (no data)
     * Use this when method returns ResponseEntity<ApiResponse<String>> and data is null
     */
    public static ResponseEntity<ApiResponse<String>> okString(String message) {
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    /**
     * Create created response (201) with data
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
    }

    /**
     * Create created response (201) with String data
     */
    public static ResponseEntity<ApiResponse<String>> createdString(String message, String data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, data));
    }

    /**
     * Create success response with custom HTTP status
     */
    public static <T> ResponseEntity<ApiResponse<T>> status(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponse.success(message, data));
    }
}