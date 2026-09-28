package com.sorascm.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, Object identifier) {
        super("RESOURCE_NOT_FOUND", "%s not found with identifier: %s".formatted(resource, identifier), HttpStatus.NOT_FOUND);
    }
}