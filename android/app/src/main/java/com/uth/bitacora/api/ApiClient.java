package com.uth.bitacora.api;

import android.content.Context;

import com.uth.bitacora.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Crea una sola instancia de Retrofit con el interceptor de autenticación. */
public final class ApiClient {

    private static BitacoraApi api;

    private ApiClient() { }

    public static synchronized BitacoraApi get(Context contexto) {
        if (api == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
            log.redactHeader("Authorization"); // el token nunca se escribe en el Logcat

            OkHttpClient cliente = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(contexto))
                    .addInterceptor(log)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            api = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BitacoraApi.class);
        }
        return api;
    }
}
