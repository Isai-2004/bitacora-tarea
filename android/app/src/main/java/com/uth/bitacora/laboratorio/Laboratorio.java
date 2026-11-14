package com.uth.bitacora.laboratorio;

/** Configuración común para que la comparación entre librerías sea justa. */
public final class Laboratorio {

    private Laboratorio() { }

    /** Tiempo de espera de conexión y de lectura, igual en las tres librerías. */
    public static final int TIEMPO_ESPERA_MS = 10_000;

    /** Etiqueta para filtrar en Logcat: tag:Comparativa */
    public static final String TAG = "Comparativa";

    /** Escenarios que se pueden provocar desde la app. API detenida y modo avión se provocan fuera de la app. */
    public enum Escenario {
        NORMAL("Normal", "api/registros", true, false),
        SIN_TOKEN("Sin token (401)", "api/registros", false, false),
        TOKEN_ALTERADO("Token alterado (401)", "api/registros", true, true),
        TIEMPO_AGOTADO("Tiempo agotado", "api/diagnostico/lento?segundos=15", true, false),
        ERROR_500("Error 500", "api/diagnostico/error", true, false);

        public final String nombre;
        public final String ruta;
        public final boolean conToken;
        public final boolean alterarToken;

        Escenario(String nombre, String ruta, boolean conToken, boolean alterarToken) {
            this.nombre = nombre;
            this.ruta = ruta;
            this.conToken = conToken;
            this.alterarToken = alterarToken;
        }

        /** Valor del encabezado Authorization para este escenario (o null si no se envía). */
        public String autorizacion(String token) {
            if (!conToken || token == null) {
                return null;
            }
            return "Bearer " + (alterarToken ? token + "x" : token);
        }
    }
}
