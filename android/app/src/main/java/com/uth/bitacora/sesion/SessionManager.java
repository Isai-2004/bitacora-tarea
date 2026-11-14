package com.uth.bitacora.sesion;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.uth.bitacora.modelos.Modelos.LoginResponse;
import com.uth.bitacora.util.Fechas;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Guarda la sesión (token, vencimiento, rol, usuario) en EncryptedSharedPreferences.
 * Las claves y los valores se cifran con AES-256; la llave maestra vive en el Android Keystore,
 * así que no se puede extraer del dispositivo aunque alguien copie el archivo de preferencias.
 */
public final class SessionManager {

    private static final String ARCHIVO = "sesion_segura";
    private static final String TOKEN = "token";
    private static final String EXPIRA = "expira_millis";
    private static final String ROL = "rol";
    private static final String USUARIO_ID = "usuario_id";
    private static final String CORREO = "correo";

    private static SessionManager instancia;
    private final SharedPreferences prefs;

    public static synchronized SessionManager get(Context contexto) {
        if (instancia == null) {
            instancia = new SessionManager(contexto.getApplicationContext());
        }
        return instancia;
    }

    private SessionManager(Context contexto) {
        prefs = crearPreferenciasCifradas(contexto);
    }

    private static SharedPreferences crearPreferenciasCifradas(Context contexto) {
        try {
            return crear(contexto);
        } catch (GeneralSecurityException | IOException e) {
            // Si la llave del Keystore se perdió (p. ej. tras restaurar el teléfono),
            // se borra el archivo y se crea uno nuevo. Nunca se usa SharedPreferences sin cifrar.
            contexto.deleteSharedPreferences(ARCHIVO);
            try {
                return crear(contexto);
            } catch (GeneralSecurityException | IOException e2) {
                throw new IllegalStateException("No se pudo crear el almacenamiento seguro de la sesión", e2);
            }
        }
    }

    private static SharedPreferences crear(Context contexto) throws GeneralSecurityException, IOException {
        MasterKey llave = new MasterKey.Builder(contexto)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                contexto,
                ARCHIVO,
                llave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public void guardar(LoginResponse login) {
        prefs.edit()
                .putString(TOKEN, login.token)
                .putLong(EXPIRA, Fechas.isoUtcAMillis(login.expira))
                .putString(ROL, login.rol)
                .putInt(USUARIO_ID, login.usuarioId)
                .putString(CORREO, login.correo)
                .apply();
    }

    public String getToken() {
        return prefs.getString(TOKEN, null);
    }

    public long getExpiraMillis() {
        return prefs.getLong(EXPIRA, 0L);
    }

    public String getRol() {
        return prefs.getString(ROL, "");
    }

    public boolean esAdmin() {
        return "Admin".equals(getRol());
    }

    public int getUsuarioId() {
        return prefs.getInt(USUARIO_ID, 0);
    }

    public String getCorreo() {
        return prefs.getString(CORREO, "");
    }

    /** Hay token y todavía no llega su fecha de vencimiento. */
    public boolean estaVigente() {
        return getToken() != null && System.currentTimeMillis() < getExpiraMillis();
    }

    /** Borra toda la sesión del dispositivo. */
    public void cerrar() {
        prefs.edit().clear().commit();
    }
}
