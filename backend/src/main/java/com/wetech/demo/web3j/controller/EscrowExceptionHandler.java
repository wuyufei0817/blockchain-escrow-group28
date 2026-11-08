package com.wetech.demo.web3j.controller;

import com.wetech.demo.web3j.exception.EscrowApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = EscrowController.class)
public class EscrowExceptionHandler {
    @ExceptionHandler(EscrowApiException.class)
    public ResponseEntity<Map<String, String>> handle(EscrowApiException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", exception.code());
        body.put("message", exception.getMessage());
        body.putAll(exception.details());
        return ResponseEntity.status(exception.status()).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> missingParameter() {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_PARAMETER", "message", "A required parameter is missing"));
    }
}
