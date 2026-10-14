package com.jpb.reconciliation.reconciliation.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;

@RestControllerAdvice
public class GlobalExceptionHandlerDuplicate {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(
            ResourceNotFoundException ex) {

        Map<String, Object> map = new HashMap<>();

        map.put("status", CommonConstants.FAILURE);
        map.put("statusMsg", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(map);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<?> handleDuplicate(
            DuplicateResourceException ex) {

        Map<String, Object> map = new HashMap<>();

        map.put("status", CommonConstants.FAILURE);
        map.put("statusMsg", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(map);
    }
}