package com.uth.bitacora.modelos;

import java.util.List;
import java.util.Map;

/** Modelos (DTO) que se envían y reciben de la API. Los nombres coinciden con el JSON. */
public final class Modelos {

    private Modelos() { }

    public static class LoginRequest {
        public final String correo;
        public final String contrasena;

        public LoginRequest(String correo, String contrasena) {
            this.correo = correo;
            this.contrasena = contrasena;
        }
    }

    public static class RegistroUsuarioRequest {
        public final String correo;
        public final String contrasena;

        public RegistroUsuarioRequest(String correo, String contrasena) {
            this.correo = correo;
            this.contrasena = contrasena;
        }
    }

    public static class LoginResponse {
        public String token;
        public String tipoToken;
        public String expira;
        public String rol;
        public int usuarioId;
        public String correo;
    }

    public static class UsuarioDto {
        public int id;
        public String correo;
        public String rol;
    }

    public static class RegistroResumen {
        public int id;
        public String titulo;
        public String descripcion;
        public boolean tieneFoto;
        public String fechaCreacion;
        public int usuarioId;
        public String creadoPor;
    }

    public static class RegistroDetalle {
        public int id;
        public String titulo;
        public String descripcion;
        public String fotoBase64;
        public String fechaCreacion;
        public int usuarioId;
        public String creadoPor;
    }

    public static class RegistroRequest {
        public final String titulo;
        public final String descripcion;
        public final String fotoBase64;

        public RegistroRequest(String titulo, String descripcion, String fotoBase64) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.fotoBase64 = fotoBase64;
        }
    }

    /** Formato estándar de error de la API (ProblemDetails). */
    public static class ProblemDetails {
        public String title;
        public String detail;
        public int status;
        public Map<String, List<String>> errors;
    }
}
