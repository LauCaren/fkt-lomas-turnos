package com.fktlomas.turnos.validation;

public final class Patrones {

    /** Dígitos, espacios, guiones, paréntesis y un + opcional al inicio. Ej: "11 5950-5888", "+54 9 11 4970-3857". */
    public static final String TELEFONO = "^\\+?[0-9\\s\\-()]{6,20}$";

    /** Igual que TELEFONO pero también acepta vacío, para campos opcionales. */
    public static final String TELEFONO_OPCIONAL = "^$|" + TELEFONO;

    private Patrones() {
    }
}
