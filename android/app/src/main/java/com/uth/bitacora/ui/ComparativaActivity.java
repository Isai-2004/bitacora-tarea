package com.uth.bitacora.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.uth.bitacora.R;
import com.uth.bitacora.laboratorio.ClienteLaboratorio;
import com.uth.bitacora.laboratorio.ClienteOkHttp;
import com.uth.bitacora.laboratorio.ClienteRetrofit;
import com.uth.bitacora.laboratorio.ClienteVolley;
import com.uth.bitacora.laboratorio.Laboratorio;
import com.uth.bitacora.laboratorio.Laboratorio.Escenario;
import com.uth.bitacora.laboratorio.Resultado;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Laboratorio: consulta GET /api/registros con Volley, OkHttp y Retrofit y muestra
 * la cantidad de registros, el tiempo en milisegundos, el hilo y los errores.
 */
public class ComparativaActivity extends AppCompatActivity {

    private static final int EJECUCIONES = 5;

    private ClienteLaboratorio volley;
    private ClienteLaboratorio okHttp;
    private ClienteLaboratorio retrofit;

    private ChipGroup grupoEscenarios;
    private TextView lblVolley;
    private TextView lblOkHttp;
    private TextView lblRetrofit;
    private TextView lblBitacora;
    private ScrollView desplazamiento;
    private final List<MaterialButton> botones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_comparativa);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        MaterialToolbar barra = findViewById(R.id.barra);
        barra.setNavigationOnClickListener(v -> finish());

        volley = new ClienteVolley(this);
        okHttp = new ClienteOkHttp(this);
        retrofit = new ClienteRetrofit(this);

        grupoEscenarios = findViewById(R.id.grupoEscenarios);
        for (Escenario e : Escenario.values()) {
            Chip chip = new Chip(this);
            chip.setText(e.nombre);
            chip.setTag(e);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            grupoEscenarios.addView(chip);
            if (e == Escenario.NORMAL) {
                chip.setChecked(true);
            }
        }

        lblVolley = findViewById(R.id.lblVolley);
        lblOkHttp = findViewById(R.id.lblOkHttp);
        lblRetrofit = findViewById(R.id.lblRetrofit);
        lblBitacora = findViewById(R.id.lblBitacora);
        desplazamiento = findViewById(R.id.desplazamiento);

        MaterialButton btnVolley = findViewById(R.id.btnVolley);
        MaterialButton btnOkHttp = findViewById(R.id.btnOkHttp);
        MaterialButton btnRetrofit = findViewById(R.id.btnRetrofit);
        MaterialButton btnMedir = findViewById(R.id.btnMedir);
        botones.add(btnVolley);
        botones.add(btnOkHttp);
        botones.add(btnRetrofit);
        botones.add(btnMedir);

        btnVolley.setOnClickListener(v -> ejecutar(volley, lblVolley));
        btnOkHttp.setOnClickListener(v -> ejecutar(okHttp, lblOkHttp));
        btnRetrofit.setOnClickListener(v -> ejecutar(retrofit, lblRetrofit));
        btnMedir.setOnClickListener(v -> medirPromedios());
        findViewById(R.id.btnLimpiar).setOnClickListener(v -> lblBitacora.setText(""));
    }

    private Escenario escenarioElegido() {
        Chip chip = findViewById(grupoEscenarios.getCheckedChipId());
        return chip == null ? Escenario.NORMAL : (Escenario) chip.getTag();
    }

    /** Una consulta con la librería indicada. */
    private void ejecutar(ClienteLaboratorio cliente, TextView etiqueta) {
        Escenario escenario = escenarioElegido();
        ocupado(true);
        agregar("→ " + cliente.nombre() + " · " + escenario.nombre + "…");
        cliente.listar(escenario, r -> {
            if (isFinishing() || isDestroyed()) return;
            ocupado(false);
            mostrar(r, etiqueta);
            if (!r.exito) {
                // Esto es lo que ve el usuario en cada escenario de falla.
                Toast.makeText(this, r.mensajeUsuario, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrar(Resultado r, TextView etiqueta) {
        etiqueta.setText(r.exito
                ? String.format(Locale.US, "%s: %d registros · %d ms · hilo %s", r.libreria, r.cantidad, r.milisegundos, r.hilo)
                : String.format(Locale.US, "%s: ✗ %s · %d ms", r.libreria, r.errorTecnico, r.milisegundos));
        agregar((r.exito ? "✓ " : "✗ ") + r.resumen());
        if (r.exito) {
            Log.i(Laboratorio.TAG, r.resumen());
        } else {
            Log.e(Laboratorio.TAG, r.resumen());
        }
    }

    // ---------- Medición: 5 ejecuciones seguidas por librería (escenario Normal) ----------

    private void medirPromedios() {
        ocupado(true);
        agregar("=== Medición: " + EJECUCIONES + " ejecuciones por librería (escenario Normal) ===");
        List<ClienteLaboratorio> clientes = new ArrayList<>();
        clientes.add(volley);
        clientes.add(okHttp);
        clientes.add(retrofit);
        medir(clientes, 0, 1, new ArrayList<>(), new StringBuilder());
    }

    private void medir(List<ClienteLaboratorio> clientes, int indiceCliente, int vuelta,
                       List<Long> tiempos, StringBuilder resumen) {
        if (indiceCliente >= clientes.size()) {
            agregar(resumen.toString());
            Log.i(Laboratorio.TAG, "PROMEDIOS\n" + resumen);
            ocupado(false);
            return;
        }
        ClienteLaboratorio cliente = clientes.get(indiceCliente);
        cliente.listar(Escenario.NORMAL, r -> {
            if (isFinishing() || isDestroyed()) return;
            agregar(String.format(Locale.US, "  %s #%d: %s", cliente.nombre(), vuelta,
                    r.exito ? r.milisegundos + " ms" : "error " + r.errorTecnico));
            Log.i(Laboratorio.TAG, r.resumen());
            if (r.exito) {
                tiempos.add(r.milisegundos);
            }
            if (vuelta < EJECUCIONES) {
                medir(clientes, indiceCliente, vuelta + 1, tiempos, resumen);
                return;
            }
            String linea = lineaPromedio(cliente.nombre(), tiempos);
            resumen.append(linea).append('\n');
            etiquetaDe(cliente).setText(linea);
            medir(clientes, indiceCliente + 1, 1, new ArrayList<>(), resumen);
        });
    }

    private static String lineaPromedio(String libreria, List<Long> tiempos) {
        if (tiempos.isEmpty()) {
            return libreria + ": sin ejecuciones exitosas";
        }
        long suma = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        for (long t : tiempos) {
            suma += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
        }
        return String.format(Locale.US, "%s: promedio %.1f ms (mín %d, máx %d) en %d ejecuciones %s",
                libreria, (double) suma / tiempos.size(), min, max, tiempos.size(), tiempos);
    }

    private TextView etiquetaDe(ClienteLaboratorio cliente) {
        if (cliente == volley) return lblVolley;
        if (cliente == okHttp) return lblOkHttp;
        return lblRetrofit;
    }

    // ---------- Utilidades de interfaz ----------

    private void ocupado(boolean activo) {
        for (MaterialButton b : botones) {
            b.setEnabled(!activo);
        }
    }

    private void agregar(String linea) {
        lblBitacora.append(linea + "\n");
        desplazamiento.post(() -> desplazamiento.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        // Cancelar lo que esté en curso para no actualizar una pantalla que ya no existe.
        if (volley != null) volley.cancelar();
        if (okHttp != null) okHttp.cancelar();
        if (retrofit != null) retrofit.cancelar();
        super.onDestroy();
    }
}
