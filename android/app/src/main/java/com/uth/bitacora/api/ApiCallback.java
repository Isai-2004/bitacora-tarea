package com.uth.bitacora.api;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback común para las llamadas a la API.
 * - 2xx  → onExito
 * - 401  → el AuthInterceptor ya cerró la sesión y abrió el login (aquí no se hace nada)
 * - 403  → onProhibido: avisa que no tiene permiso, SIN cerrar la sesión
 * - otro → onError
 */
public abstract class ApiCallback<T> implements Callback<T> {

    protected final Activity actividad;

    protected ApiCallback(Activity actividad) {
        this.actividad = actividad;
    }

    public abstract void onExito(T cuerpo);

    public void onError(ErrorApi error) {
        Toast.makeText(actividad, error.mensaje(), Toast.LENGTH_LONG).show();
    }

    public void onProhibido(ErrorApi error) {
        String detalle = error.detalle.isEmpty() ? "" : "\n" + error.detalle;
        Toast.makeText(actividad, "No tienes permiso para realizar esta acción." + detalle, Toast.LENGTH_LONG).show();
    }

    /** Solo se usa en el login, donde 401 significa credenciales incorrectas. */
    public void onNoAutenticado(ErrorApi error) { }

    /** Se llama siempre al final (para ocultar el indicador de carga). */
    public void onTerminado() { }

    @Override
    public void onResponse(@NonNull Call<T> llamada, @NonNull Response<T> respuesta) {
        if (actividad.isFinishing() || actividad.isDestroyed()) {
            return;
        }
        onTerminado();
        if (respuesta.isSuccessful()) {
            onExito(respuesta.body());
            return;
        }
        ErrorApi error = ErrorApi.desde(respuesta);
        switch (respuesta.code()) {
            case 401:
                onNoAutenticado(error);
                break;
            case 403:
                onProhibido(error);
                break;
            default:
                onError(error);
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> llamada, @NonNull Throwable t) {
        if (actividad.isFinishing() || actividad.isDestroyed()) {
            return;
        }
        onTerminado();
        onError(ErrorApi.sinConexion());
    }
}
