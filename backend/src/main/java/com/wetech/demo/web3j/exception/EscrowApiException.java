package com.wetech.demo.web3j.exception;

import java.util.Map;

public class EscrowApiException extends RuntimeException {
    private final int status;
    private final String code;
    private final Map<String, String> details;

    public EscrowApiException(int status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public EscrowApiException(int status, String code, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public int status() { return status; }
    public String code() { return code; }
    public Map<String, String> details() { return details; }
}
