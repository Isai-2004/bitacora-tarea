package com.uth.bitacora;

import android.app.Application;

import com.uth.bitacora.sesion.SessionManager;

public class BitacoraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Prepara el almacenamiento cifrado de la sesión al iniciar la app.
        SessionManager.get(this);
    }
}
