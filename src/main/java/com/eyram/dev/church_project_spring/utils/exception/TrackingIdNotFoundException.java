package com.eyram.dev.church_project_spring.utils.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class TrackingIdNotFoundException extends RuntimeException{
    private static final long serialVersionUID = 1L;
    public TrackingIdNotFoundException(String message) {
        super(message);
    }
}