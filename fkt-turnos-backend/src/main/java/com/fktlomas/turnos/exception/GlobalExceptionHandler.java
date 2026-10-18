package com.fktlomas.turnos.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Datos inválidos", errores));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String campo = v.getPropertyPath().toString();
            campo = campo.contains(".") ? campo.substring(campo.lastIndexOf('.') + 1) : campo;
            errores.put(campo, v.getMessage());
        });
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Datos inválidos", errores));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage(), null));
    }

    @ExceptionHandler(TurnoNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleTurnoNoDisponible(TurnoNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo(HttpStatus.CONFLICT, ex.getMessage(), null));
    }

    @ExceptionHandler(ConflictoDeDatosException.class)
    public ResponseEntity<Map<String, Object>> handleConflicto(ConflictoDeDatosException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo(HttpStatus.CONFLICT, ex.getMessage(), null));
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleArchivoInvalido(ArchivoInvalidoException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, ex.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(cuerpo(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (5MB)", null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpo(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", null));
    }

    /** Cuenta de staff desactivada por un admin (ver Usuario.activo / AppUserDetails.isEnabled()). */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpo(HttpStatus.UNAUTHORIZED, "Esta cuenta está desactivada", null));
    }

    /**
     * Autenticado pero sin el rol necesario para este endpoint puntual — la vía @PreAuthorize
     * a nivel de método/clase que separa ADMIN de SECRETARIA dentro de /api/admin/** (ver
     * SecurityConfig y los controllers admin). Sin este handler explícito, Spring MVC deja que
     * la excepción se resuelva por el camino por defecto de Spring Security — que en este setup
     * (con @EnableMethodSecurity + un AuthenticationEntryPoint custom) termina devolviendo 401
     * en vez de 403 para este caso puntual, y el frontend trata cualquier 401 como "la sesión
     * venció" y desloguea — así que una secretaria autenticada que pisa un endpoint ADMIN-only
     * quedaba deslogueada en el acto en vez de simplemente ver un botón/pantalla sin permiso.
     * Verificado en vivo: sin este handler, GET /api/admin/medicos con un token de SECRETARIA
     * válido devolvía 401 con el mensaje de "sesión vencida", no 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(cuerpo(HttpStatus.FORBIDDEN, "No tenés permiso para hacer esto", null));
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<Map<String, Object>> handleDemasiadosIntentos(DemasiadosIntentosException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(cuerpo(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), null));
    }

    @ExceptionHandler(MfaRequeridaException.class)
    public ResponseEntity<Map<String, Object>> handleMfaRequerida(MfaRequeridaException ex) {
        Map<String, Object> body = cuerpo(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
        body.put("requiereMfa", true);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, mensajeSeguro(ex, "Datos inválidos"), null));
    }

    /** Ej.: un canal de notificación sin credenciales configuradas (ver WhatsAppService), o un envío que falló. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, mensajeSeguro(ex, "No se pudo completar la operación"), null));
    }

    /**
     * Choque contra una restricción única de la base (ej: dos pedidos concurrentes con el
     * mismo DNI o email) que ya pasó las validaciones de arriba porque, hasta el instante
     * del INSERT, todavía no existía el conflicto. Sin este handler cae en el 500 genérico
     * de Spring Boot — no filtra detalles internos en ningún caso, pero un 409 es más claro
     * para quien está usando la API.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpo(HttpStatus.CONFLICT, "Ya existe un registro con esos datos", null));
    }

    /**
     * IllegalArgumentException/IllegalStateException se usan en todo el código propio como
     * "error de negocio con un mensaje para mostrarle a la persona" (en castellano, pensado
     * para la pantalla). Pero también las tiran librerías de terceros (Spring, Jackson, el SDK
     * de Mercado Pago, Enum.valueOf...) con mensajes técnicos que nombran clases internas o
     * datos del request — no tienen que llegar al navegador. Se muestra el mensaje solo si la
     * excepción nació en código de este proyecto (com.fktlomas.*); si no, uno genérico.
     */
    private static String mensajeSeguro(RuntimeException ex, String generico) {
        StackTraceElement[] traza = ex.getStackTrace();
        boolean propia = traza.length > 0 && traza[0].getClassName().startsWith("com.fktlomas.");
        return propia && ex.getMessage() != null ? ex.getMessage() : generico;
    }

    private Map<String, Object> cuerpo(HttpStatus status, String mensaje, Object detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("mensaje", mensaje);
        if (detalle != null) {
            body.put("detalle", detalle);
        }
        return body;
    }
}
