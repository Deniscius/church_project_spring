package com.eyram.dev.church_project_spring.utils.exception.response;

public class MessageResponse {

    private Boolean status;
    private String message;

    public MessageResponse() {
    }

    public MessageResponse(Boolean status, String message) {
        this.status = status;
        this.message = message;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}