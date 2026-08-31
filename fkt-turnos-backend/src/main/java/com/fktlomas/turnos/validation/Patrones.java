package com.fktlomas.turnos.validation;

public final class Patrones {

    /** Dígitos, espacios, guiones, paréntesis y un + opcional al inicio. Ej: "11 5950-5888", "+54 9 11 4970-3857". */
    public static final String TELEFONO = "^\\+?[0-9\\s\\-()]{6,20}$";

    /** Igual que TELEFONO pero también acepta vacío, para campos opcionales. */
    public static final String TELEFONO_OPCIONAL = "^$|" + TELEFONO;

    /** Mínimo 8 caracteres, con al menos una mayúscula, una minúscula, un número y un símbolo. */
    public static final String PASSWORD_FUERTE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    public static final String PASSWORD_FUERTE_MENSAJE =
            "La contraseña debe tener al menos 8 caracteres, con mayúsculas, minúsculas, números y un símbolo";

    private Patrones() {
    }
}
