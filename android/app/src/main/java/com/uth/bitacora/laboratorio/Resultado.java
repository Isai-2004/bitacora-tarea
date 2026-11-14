package com.uth.bitacora.laboratorio;

import java.util.Locale;

/** Resultado de una llamada de prueba. */
public class Resultado {

    public final String libreria;
    public final Laboratorio.Escenario escenario;
    public final boolean exito;
    /** Código HTTP, o 0 si no hubo respuesta del servidor. */
    public final int codigoHttp;
    public final int cantidad;
    public final long milisegundos;
    /** Hilo en el que llegó la respuesta (antes de pasar a la interfaz). */
    public final String hilo;
    /** Lo que recibe el código: tipo de excepción o error. */
    public final String errorTecnico;
    /** Lo que ve el usuario. */
    public final String mensajeUsuario;

    private Resultado(String libreria, Laboratorio.Escenario escenario, boolean exito, int codigoHttp, int cantidad,
                      long milisegundos, String hilo, String errorTecnico, String mensajeUsuario) {
        this.libreria = libreria;
        this.escenario = escenario;
        this.exito = exito;
        this.codigoHttp = codigoHttp;
        this.cantidad = cantidad;
        this.milisegundos = milisegundos;
        this.hilo = hilo;
        this.errorTecnico = errorTecnico;
        this.mensajeUsuario = mensajeUsuario;
    }

    public static Resultado exito(String libreria, Laboratorio.Escenario escenario, int codigo, int cantidad,
                                  long ms, String hilo) {
        return new Resultado(libreria, escenario, true, codigo, cantidad, ms, hilo, "", "");
    }

    public static Resultado error(String libreria, Laboratorio.Escenario escenario, int codigo, long ms, String hilo,
                                  ClasificadorErrores.Categoria categoria, String errorTecnico) {
        return new Resultado(libreria, escenario, false, codigo, 0, ms, hilo, errorTecnico, categoria.mensaje);
    }

    /** Línea para la pantalla y para Logcat. */
    public String resumen() {
        if (exito) {
            return String.format(Locale.US, "%s | %s | HTTP %d | %d registros | %d ms | hilo: %s",
                    libreria, escenario.nombre, codigoHttp, cantidad, milisegundos, hilo);
        }
        return String.format(Locale.US, "%s | %s | %s | %d ms | hilo: %s | código: %s | usuario: %s",
                libreria, escenario.nombre, codigoHttp == 0 ? "sin respuesta" : "HTTP " + codigoHttp,
                milisegundos, hilo, errorTecnico, mensajeUsuario);
    }
}
