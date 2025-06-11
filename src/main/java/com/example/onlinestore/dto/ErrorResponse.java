package com.example.onlinestore.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    // Default constructor for Jackson deserialization if needed
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this(); // Call default constructor to set timestamp
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Constructor that was originally present, could be kept for compatibility or removed
    // For now, let's keep it but ensure timestamp is also set.
    public ErrorResponse(String message) {
        this();
        this.message = message;
        // Default status/error/path would be 0/null/null if this constructor is used.
        // This might be okay if only a message is relevant for some simple errors.
        // However, the main constructor is preferred.
    }

    // Getters and Setters

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
