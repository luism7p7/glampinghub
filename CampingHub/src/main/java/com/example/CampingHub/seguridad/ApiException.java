package com.example.CampingHub.seguridad;

import org.springframework.http.HttpStatus;

/** Error de negocio con el código HTTP que debe recibir el cliente. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ApiException badRequest(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    public static ApiException notFound(String m) { return new ApiException(HttpStatus.NOT_FOUND, m); }
    public static ApiException forbidden(String m) { return new ApiException(HttpStatus.FORBIDDEN, m); }
    public static ApiException conflict(String m) { return new ApiException(HttpStatus.CONFLICT, m); }
}
