package com.uth.bitacora.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** Navegación común relacionada con la sesión. */
public final class Navegacion {

    public static final String EXTRA_MENSAJE = "mensaje";

    private Navegacion() { }

    /** Abre el inicio de sesión, borrando la pila de pantallas, y muestra un mensaje. */
    public static void irAlLogin(Context contexto, String mensaje) {
        Context app = contexto.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(app, LoginActivity.class)
                    .putExtra(EXTRA_MENSAJE, mensaje)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(intent);
        });
    }
}
