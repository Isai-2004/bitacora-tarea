package com.uth.bitacora.laboratorio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.uth.bitacora.BuildConfig;
import com.uth.bitacora.modelos.Modelos.RegistroResumen;
import com.uth.bitacora.sesion.SessionManager;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * GET /api/registros con OkHttp.
 * - El Request se construye a mano, incluido el encabezado Authorization.
 * - enqueue() lo ejecuta de forma asíncrona.
 * - El JSON se convierte a objetos con Gson.
 * - Las respuestas llegan en un HILO DE FONDO de OkHttp: hay que pasar al hilo
 *   principal (Handler del Looper principal) antes de tocar la interfaz.
 */
public class ClienteOkHttp implements ClienteLaboratorio {

    private static final Type TIPO_LISTA = new TypeToken<List<RegistroResumen>>() { }.getType();

    private final Context contexto;
    private final SessionManager sesion;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final OkHttpClient cliente = new OkHttpClient.Builder()
            .connectTimeout(Laboratorio.TIEMPO_ESPERA_MS, TimeUnit.MILLISECONDS)
            .readTimeout(Laboratorio.TIEMPO_ESPERA_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(Laboratorio.TIEMPO_ESPERA_MS, TimeUnit.MILLISECONDS)
            .build();

    public ClienteOkHttp(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.sesion = SessionManager.get(this.contexto);
    }

    @Override
    public String nombre() {
        return "OkHttp";
    }

    @Override
    public void listar(Laboratorio.Escenario escenario, AlTerminar alTerminar) {
        final long inicio = SystemClock.elapsedRealtime();

        Request.Builder constructor = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + escenario.ruta)
                .header("Accept", "application/json")
                .get();
        String autorizacion = escenario.autorizacion(sesion.getToken());
        if (autorizacion != null) {
            constructor.header("Authorization", autorizacion);
        }

        cliente.newCall(constructor.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call llamada, @NonNull IOException e) {
                // Sin respuesta del servidor: sin red, API detenida, tiempo agotado o cancelada.
                long ms = SystemClock.elapsedRealtime() - inicio;
                String hilo = Thread.currentThread().getName();
                Log.w(Laboratorio.TAG, "OkHttp onFailure", e);
                Resultado r = Resultado.error(nombre(), escenario, 0, ms, hilo,
                        ClasificadorErrores.porExcepcion(contexto, e), ClasificadorErrores.describir(e));
                hiloPrincipal.post(() -> alTerminar.resultado(r));
            }

            @Override
            public void onResponse(@NonNull Call llamada, @NonNull Response respuesta) {
                String hilo = Thread.currentThread().getName();
                Resultado r;
                // try-with-resources: el cuerpo se cierra siempre.
                try (ResponseBody cuerpo = respuesta.body()) {
                    String json = cuerpo == null ? "" : cuerpo.string();
                    long ms = SystemClock.elapsedRealtime() - inicio;
                    if (!respuesta.isSuccessful()) {
                        // OkHttp NO trata 401 o 500 como falla: hay que revisar el código.
                        r = Resultado.error(nombre(), escenario, respuesta.code(), ms, hilo,
                                ClasificadorErrores.porCodigo(respuesta.code()), "HTTP " + respuesta.code());
                    } else {
                        List<RegistroResumen> registros = gson.fromJson(json, TIPO_LISTA);
                        r = Resultado.exito(nombre(), escenario, respuesta.code(),
                                registros == null ? 0 : registros.size(), ms, hilo);
                    }
                } catch (IOException | JsonSyntaxException e) {
                    long ms = SystemClock.elapsedRealtime() - inicio;
                    r = Resultado.error(nombre(), escenario, respuesta.code(), ms, hilo,
                            ClasificadorErrores.porExcepcion(contexto, e), ClasificadorErrores.describir(e));
                }
                final Resultado resultado = r;
                hiloPrincipal.post(() -> alTerminar.resultado(resultado));
            }
        });
    }

    @Override
    public void cancelar() {
        cliente.dispatcher().cancelAll();
    }
}
