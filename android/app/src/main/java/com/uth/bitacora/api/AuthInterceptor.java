package com.uth.bitacora.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.uth.bitacora.sesion.SessionManager;
import com.uth.bitacora.ui.Navegacion;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor de OkHttp:
 * 1. Agrega "Authorization: Bearer <token>" a cada petición (menos login y registro).
 * 2. Si la API responde 401 (token vencido, alterado o ausente), borra la sesión
 *    y regresa a la pantalla de inicio de sesión con un mensaje.
 *    El 403 NO cierra la sesión: lo maneja cada pantalla (ApiCallback.onProhibido).
 */
public class AuthInterceptor implements Interceptor {

    private final Context contexto;
    private final SessionManager sesion;

    public AuthInterceptor(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.sesion = SessionManager.get(contexto);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request peticion = chain.request();
        boolean esAutenticacion = peticion.url().encodedPath().startsWith("/api/auth/");

        String token = sesion.getToken();
        if (!esAutenticacion && token != null) {
            peticion = peticion.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }

        Response respuesta = chain.proceed(peticion);

        if (respuesta.code() == 401 && !esAutenticacion) {
            String mensaje = ErrorApi.desdeCuerpo(respuesta.peekBody(4096).string()).mensaje();
            sesion.cerrar();
            Navegacion.irAlLogin(contexto, mensaje.isEmpty()
                    ? "Tu sesión terminó. Inicia sesión de nuevo."
                    : mensaje);
        }
        return respuesta;
    }
}
