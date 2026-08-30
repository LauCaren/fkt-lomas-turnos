package com.fktlomas.turnos.exception;

public class MfaRequeridaException extends RuntimeException {
    public MfaRequeridaException() {
        super("Ingresá el código de tu app de autenticación");
    }
}
