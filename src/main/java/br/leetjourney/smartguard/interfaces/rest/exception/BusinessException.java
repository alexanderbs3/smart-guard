package br.leetjourney.smartguard.interfaces.rest.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
