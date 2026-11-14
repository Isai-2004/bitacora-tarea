package com.uth.bitacora.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uth.bitacora.R;
import com.uth.bitacora.api.ApiCallback;
import com.uth.bitacora.api.ApiClient;
import com.uth.bitacora.modelos.Modelos.RegistroResumen;
import com.uth.bitacora.sesion.SessionManager;
import com.uth.bitacora.util.Fechas;

import java.util.List;

/** Lista de registros de la bitácora. */
public class MainActivity extends AppCompatActivity implements RegistrosAdapter.Acciones {

    private SessionManager sesion;
    private RegistrosAdapter adaptador;
    private ProgressBar progreso;
    private TextView lblVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sesion = SessionManager.get(this);
        if (!sesion.estaVigente()) {
            sesion.cerrar();
            Navegacion.irAlLogin(this, getString(R.string.sesion_vencida));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        MaterialToolbar barra = findViewById(R.id.barra);
        setSupportActionBar(barra);
        barra.setSubtitle(getString(R.string.subtitulo_sesion, sesion.getCorreo(), sesion.getRol(),
                Fechas.horaLocal(sesion.getExpiraMillis())));

        progreso = findViewById(R.id.progreso);
        lblVacio = findViewById(R.id.lblVacio);

        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new RegistrosAdapter(this, sesion.esAdmin(), sesion.getUsuarioId());
        lista.setAdapter(adaptador);

        findViewById(R.id.btnNuevo).setOnClickListener(v ->
                startActivity(new Intent(this, FormularioRegistroActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adaptador != null) {
            cargar();
        }
    }

    private void cargar() {
        progreso.setVisibility(View.VISIBLE);
        ApiClient.get(this).listar().enqueue(new ApiCallback<List<RegistroResumen>>(this) {
            @Override
            public void onExito(List<RegistroResumen> registros) {
                adaptador.setDatos(registros);
                lblVacio.setVisibility(registros == null || registros.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onTerminado() {
                progreso.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void editar(RegistroResumen registro) {
        startActivity(new Intent(this, FormularioRegistroActivity.class)
                .putExtra(FormularioRegistroActivity.EXTRA_ID, registro.id));
    }

    @Override
    public void eliminar(RegistroResumen registro) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirmar_eliminar_titulo)
                .setMessage(getString(R.string.confirmar_eliminar_mensaje, registro.titulo))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> confirmarEliminar(registro))
                .show();
    }

    private void confirmarEliminar(RegistroResumen registro) {
        progreso.setVisibility(View.VISIBLE);
        ApiClient.get(this).eliminar(registro.id).enqueue(new ApiCallback<Void>(this) {
            @Override
            public void onExito(Void vacio) {
                Toast.makeText(MainActivity.this, R.string.registro_eliminado, Toast.LENGTH_SHORT).show();
                cargar();
            }

            @Override
            public void onTerminado() {
                progreso.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.accion_recargar) {
            cargar();
            return true;
        }
        if (item.getItemId() == R.id.accion_cerrar_sesion) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cerrar_sesion)
                    .setMessage(R.string.confirmar_cerrar_sesion)
                    .setNegativeButton(R.string.cancelar, null)
                    .setPositiveButton(R.string.cerrar_sesion, (d, w) -> {
                        sesion.cerrar();
                        Navegacion.irAlLogin(this, getString(R.string.sesion_cerrada));
                    })
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
