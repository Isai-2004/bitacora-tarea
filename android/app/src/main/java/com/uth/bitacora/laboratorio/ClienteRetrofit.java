package com.uth.bitacora.laboratorio;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.uth.bitacora.BuildConfig;
import com.uth.bitacora.api.BitacoraApi;
import com.uth.bitacora.modelos.Modelos.RegistroResumen;
import com.uth.bitacora.sesion.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * GET /api/registros con Retrofit, reutilizando la interfaz BitacoraApi y los modelos de la app.
 * Para el laboratorio se usa una instancia con los mismos tiempos de espera que las otras librerías
 * y SIN el AuthInterceptor de la app (que ante un 401 cerraría la sesión), para poder observar el error.
 * Las respuestas llegan en el HILO PRINCIPAL (Retrofit las entrega ahí en Android).
 */
public class ClienteRetrofit implements ClienteLaboratorio {

    private final Context contexto;
    private final SessionManager sesion;
    private final BitacoraApi api;
    private final List<Call<?>> llamadas = new ArrayList<>();

    public ClienteRetrofit(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.sesion = SessionManager.get(this.contexto);

        OkHttpClient cliente = new OkHttpClient.Builder()
                .connectTimeout(Laboratorio.TIEMPO_ESPERA_MS, TimeUnit.MILLISECONDS)
                .readTimeout(Laboratorio.TIEMPO_ESPERA_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(Laboratorio.TIEMPO_ESPERA_MS, TimeUnit.MILLISECONDS)
                .build();

        this.api = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BitacoraApi.class);
    }

    @Override
    public String nombre() {
        return "Retrofit";
    }

    @Override
    public void listar(Laboratorio.Escenario escenario, AlTerminar alTerminar) {
        final long inicio = SystemClock.elapsedRealtime();
        Call<List<RegistroResumen>> llamada =
                api.listarLaboratorio(escenario.ruta, escenario.autorizacion(sesion.getToken()));
        llamadas.add(llamada);

        llamada.enqueue(new Callback<List<RegistroResumen>>() {
            @Override
            public void onResponse(@NonNull Call<List<RegistroResumen>> c, @NonNull Response<List<RegistroResumen>> r) {
                llamadas.remove(c);
                long ms = SystemClock.elapsedRealtime() - inicio;
                String hilo = Thread.currentThread().getName();
                if (r.isSuccessful()) {
                    List<RegistroResumen> registros = r.body();
                    alTerminar.resultado(Resultado.exito(nombre(), escenario, r.code(),
                            registros == null ? 0 : registros.size(), ms, hilo));
                } else {
                    // Igual que OkHttp: un 401 o 500 llega a onResponse, no a onFailure.
                    alTerminar.resultado(Resultado.error(nombre(), escenario, r.code(), ms, hilo,
                            ClasificadorErrores.porCodigo(r.code()), "HTTP " + r.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RegistroResumen>> c, @NonNull Throwable t) {
                llamadas.remove(c);
                if (c.isCanceled()) {
                    return;
                }
                long ms = SystemClock.elapsedRealtime() - inicio;
                Log.w(Laboratorio.TAG, "Retrofit onFailure", t);
                alTerminar.resultado(Resultado.error(nombre(), escenario, 0, ms, Thread.currentThread().getName(),
                        ClasificadorErrores.porExcepcion(contexto, t), ClasificadorErrores.describir(t)));
            }
        });
    }

    @Override
    public void cancelar() {
        for (Call<?> c : new ArrayList<>(llamadas)) {
            c.cancel();
        }
        llamadas.clear();
    }
}
