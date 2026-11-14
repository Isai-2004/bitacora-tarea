package com.uth.bitacora.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Conversión de las fechas ISO 8601 en UTC que devuelve la API. */
public final class Fechas {

    private Fechas() { }

    /** "2026-11-11T14:05:00.123Z" → milisegundos. Devuelve 0 si no se puede leer. */
    public static long isoUtcAMillis(String iso) {
        if (iso == null || iso.length() < 19) {
            return 0L;
        }
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date fecha = formato.parse(iso.substring(0, 19));
            return fecha == null ? 0L : fecha.getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    /** Fecha y hora local del teléfono, por ejemplo "11/11/2026 08:05". */
    public static String fechaHoraLocal(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    public static String horaLocal(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
    }

    public static String isoAFechaLocal(String iso) {
        long millis = isoUtcAMillis(iso);
        return millis == 0L ? "" : fechaHoraLocal(millis);
    }
}
