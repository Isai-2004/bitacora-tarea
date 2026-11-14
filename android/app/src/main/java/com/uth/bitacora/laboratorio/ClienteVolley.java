package com.uth.bitacora.laboratorio;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.uth.bitacora.BuildConfig;
import com.uth.bitacora.laboratorio.ClasificadorErrores.Categoria;
import com.uth.bitacora.sesion.SessionManager;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/registros con Volley.
 * - JsonArrayRequest convierte la respuesta en un JSONArray.
 * - El token se envía sobrescribiendo getHeaders().
 * - DefaultRetryPolicy define el tiempo de espera y los reintentos.
 * - Las respuestas llegan en el HILO PRINCIPAL: se puede actualizar la interfaz directamente.
 */
public class ClienteVolley implements ClienteLaboratorio {

    private static final String ETIQUETA = "laboratorio";

    private final Context contexto;
    private final RequestQueue cola;
    private final SessionManager sesion;

    public ClienteVolley(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.cola = Volley.newRequestQueue(this.contexto);
        this.sesion = SessionManager.get(this.contexto);
    }

    @Override
    public String nombre() {
        return "Volley";
    }

    @Override
    public void listar(Laboratorio.Escenario escenario, AlTerminar alTerminar) {
        final long inicio = SystemClock.elapsedRealtime();
        final String autorizacion = escenario.autorizacion(sesion.getToken());

        JsonArrayRequest peticion = new JsonArrayRequest(
                Request.Method.GET,
                BuildConfig.API_BASE_URL + escenario.ruta,
                (JSONArray) null,
                arreglo -> alTerminar.resultado(Resultado.exito(nombre(), escenario, 200, arreglo.length(),
                        SystemClock.elapsedRealtime() - inicio, Thread.currentThread().getName())),
                error -> {
                    long ms = SystemClock.elapsedRealtime() - inicio;
                    int codigo = error.networkResponse != null ? error.networkResponse.statusCode : 0;
                    Log.w(Laboratorio.TAG, "Volley recibió " + error.getClass().getSimpleName(), error);
                    alTerminar.resultado(Resultado.error(nombre(), escenario, codigo, ms,
                            Thread.currentThread().getName(), clasificar(error, codigo),
                            error.getClass().getSimpleName() + (codigo > 0 ? " (HTTP " + codigo + ")" : "")));
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> encabezados = new HashMap<>();
                encabezados.put("Accept", "application/json");
                if (autorizacion != null) {
                    encabezados.put("Authorization", autorizacion);
                }
                return encabezados;
            }
        };

        // Tiempo de espera de 10 s, sin reintentos (para medir igual que OkHttp y Retrofit).
        peticion.setRetryPolicy(new DefaultRetryPolicy(Laboratorio.TIEMPO_ESPERA_MS, 0, 1f));
        peticion.setShouldCache(false);
        peticion.setTag(ETIQUETA);
        cola.add(peticion);
    }

    private Categoria clasificar(VolleyError error, int codigo) {
        if (codigo > 0) return ClasificadorErrores.porCodigo(codigo);
        if (!ClasificadorErrores.hayRed(contexto)) return Categoria.SIN_RED;
        if (error instanceof TimeoutError) return Categoria.TIEMPO_AGOTADO;
        if (error instanceof NoConnectionError) return Categoria.API_NO_DISPONIBLE;
        if (error instanceof AuthFailureError) return Categoria.NO_AUTENTICADO;
        if (error instanceof ServerError) return Categoria.ERROR_SERVIDOR;
        if (error instanceof ParseError) return Categoria.RESPUESTA_INVALIDA;
        return Categoria.DESCONOCIDO;
    }

    @Override
    public void cancelar() {
        cola.cancelAll(ETIQUETA);
    }
}
