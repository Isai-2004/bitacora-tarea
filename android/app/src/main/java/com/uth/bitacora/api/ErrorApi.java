package com.uth.bitacora.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.uth.bitacora.modelos.Modelos.ProblemDetails;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;

/** Error devuelto por la API (lee el ProblemDetails del cuerpo). */
public class ErrorApi {

    public final int status;
    public final String titulo;
    public final String detalle;
    public final Map<String, List<String>> errores;

    private ErrorApi(int status, String titulo, String detalle, Map<String, List<String>> errores) {
        this.status = status;
        this.titulo = titulo == null ? "" : titulo;
        this.detalle = detalle == null ? "" : detalle;
        this.errores = errores == null ? Collections.emptyMap() : errores;
    }

    public static ErrorApi desde(Response<?> respuesta) {
        String cuerpo = "";
        ResponseBody error = respuesta.errorBody();
        if (error != null) {
            try {
                cuerpo = error.string();
            } catch (IOException ignorado) {
                // sin cuerpo
            }
        }
        ErrorApi leido = desdeCuerpo(cuerpo);
        return new ErrorApi(respuesta.code(), leido.titulo, leido.detalle, leido.errores);
    }

    public static ErrorApi desdeCuerpo(String json) {
        try {
            ProblemDetails p = new Gson().fromJson(json, ProblemDetails.class);
            if (p != null) {
                return new ErrorApi(p.status, p.title, p.detail, p.errors);
            }
        } catch (JsonSyntaxException ignorado) {
            // no era JSON
        }
        return new ErrorApi(0, "", "", null);
    }

    public static ErrorApi sinConexion() {
        return new ErrorApi(0, "Sin conexión",
                "No se pudo conectar con la API. Verifica que esté en ejecución.", null);
    }

    /** Primer error del campo indicado (sin distinguir mayúsculas), o null. */
    public String errorDe(String campo) {
        for (Map.Entry<String, List<String>> e : errores.entrySet()) {
            if (e.getKey().equalsIgnoreCase(campo) && e.getValue() != null && !e.getValue().isEmpty()) {
                return e.getValue().get(0);
            }
        }
        return null;
    }

    /** Mensaje legible para el usuario. */
    public String mensaje() {
        if (!detalle.isEmpty()) {
            return detalle;
        }
        for (List<String> lista : errores.values()) {
            if (lista != null && !lista.isEmpty()) {
                return lista.get(0);
            }
        }
        return titulo;
    }
}
