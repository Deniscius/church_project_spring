package com.eyram.dev.church_project_spring.utils.exception;

import java.util.Date;


public class ErrorMessage {
    private int statusCode;
    private Date timestamp;
    private String message;
    private String details;
    private String traceId;

    public ErrorMessage() {
    }

    public ErrorMessage(int statusCode, Date timestamp, String message, String details) {
        this(statusCode, timestamp, message, details, null);
    }

    public ErrorMessage(int statusCode, Date timestamp, String message, String details, String traceId) {
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.traceId = traceId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
