package com.uth.bitacora.laboratorio;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/** Traduce los errores de las tres librerías a categorías comunes y a un mensaje para el usuario. */
public final class ClasificadorErrores {

    private ClasificadorErrores() { }

    public enum Categoria {
        SIN_RED("No hay conexión a internet. Revisa el Wi-Fi o desactiva el modo avión."),
        API_NO_DISPONIBLE("No se pudo conectar con el servidor. Intenta más tarde."),
        TIEMPO_AGOTADO("El servidor tardó demasiado en responder. Intenta de nuevo."),
        NO_AUTENTICADO("Tu sesión no es válida o venció. Inicia sesión de nuevo."),
        PROHIBIDO("No tienes permiso para realizar esta acción."),
        ERROR_SERVIDOR("Ocurrió un error en el servidor. Intenta más tarde."),
        RESPUESTA_INVALIDA("La respuesta del servidor no tiene el formato esperado."),
        DESCONOCIDO("Ocurrió un error inesperado.");

        public final String mensaje;

        Categoria(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    /** ¿El dispositivo tiene una red activa? (falso en modo avión). */
    public static boolean hayRed(Context contexto) {
        ConnectivityManager cm = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return true;
        }
        Network red = cm.getActiveNetwork();
        NetworkCapabilities capacidades = red == null ? null : cm.getNetworkCapabilities(red);
        return capacidades != null;
    }

    /** Clasifica por código HTTP (cuando el servidor sí respondió). */
    public static Categoria porCodigo(int codigo) {
        if (codigo == 401) return Categoria.NO_AUTENTICADO;
        if (codigo == 403) return Categoria.PROHIBIDO;
        if (codigo >= 500) return Categoria.ERROR_SERVIDOR;
        return Categoria.DESCONOCIDO;
    }

    /** Clasifica excepciones de red de OkHttp y Retrofit (cuando no hubo respuesta). */
    public static Categoria porExcepcion(Context contexto, Throwable t) {
        if (!hayRed(contexto)) return Categoria.SIN_RED;
        if (t instanceof SocketTimeoutException) return Categoria.TIEMPO_AGOTADO;
        if (t instanceof InterruptedIOException && String.valueOf(t.getMessage()).contains("timeout")) {
            return Categoria.TIEMPO_AGOTADO;
        }
        if (t instanceof ConnectException || t instanceof NoRouteToHostException) return Categoria.API_NO_DISPONIBLE;
        if (t instanceof UnknownHostException) return Categoria.SIN_RED;
        if (t instanceof com.google.gson.JsonParseException) return Categoria.RESPUESTA_INVALIDA;
        return Categoria.DESCONOCIDO;
    }

    public static String describir(Throwable t) {
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }
}
