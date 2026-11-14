package com.uth.bitacora.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.uth.bitacora.R;
import com.uth.bitacora.api.ApiCallback;
import com.uth.bitacora.api.ApiClient;
import com.uth.bitacora.api.ErrorApi;
import com.uth.bitacora.modelos.Modelos.RegistroDetalle;
import com.uth.bitacora.modelos.Modelos.RegistroRequest;
import com.uth.bitacora.sesion.SessionManager;
import com.uth.bitacora.util.Fechas;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;

/** Crear o editar un registro. La descripción se puede dictar por voz. */
public class FormularioRegistroActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    private int id;
    private String fotoActual;
    private TextInputLayout capaTitulo;
    private TextInputLayout capaDescripcion;
    private TextInputEditText txtTitulo;
    private TextInputEditText txtDescripcion;
    private TextView lblInfo;
    private MaterialButton btnGuardar;
    private ProgressBar progreso;

    private final ActivityResultLauncher<Intent> dictado = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != Activity.RESULT_OK || resultado.getData() == null) {
                    return;
                }
                ArrayList<String> textos = resultado.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (textos != null && !textos.isEmpty()) {
                    String actual = txtDescripcion.getText() == null ? "" : txtDescripcion.getText().toString().trim();
                    txtDescripcion.setText(actual.isEmpty() ? textos.get(0) : actual + " " + textos.get(0));
                    txtDescripcion.setSelection(txtDescripcion.length());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_formulario_registro);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        id = getIntent().getIntExtra(EXTRA_ID, 0);

        MaterialToolbar barra = findViewById(R.id.barra);
        barra.setTitle(id == 0 ? R.string.nuevo_registro : R.string.editar_registro);
        barra.setNavigationOnClickListener(v -> finish());

        capaTitulo = findViewById(R.id.capaTitulo);
        capaDescripcion = findViewById(R.id.capaDescripcion);
        txtTitulo = findViewById(R.id.txtTitulo);
        txtDescripcion = findViewById(R.id.txtDescripcion);
        lblInfo = findViewById(R.id.lblInfo);
        btnGuardar = findViewById(R.id.btnGuardar);
        progreso = findViewById(R.id.progreso);

        capaDescripcion.setEndIconOnClickListener(v -> dictar());
        btnGuardar.setOnClickListener(v -> guardar());

        if (id != 0) {
            cargar();
        }
    }

    private void cargar() {
        cargando(true);
        ApiClient.get(this).obtener(id).enqueue(new ApiCallback<RegistroDetalle>(this) {
            @Override
            public void onExito(RegistroDetalle r) {
                txtTitulo.setText(r.titulo);
                txtDescripcion.setText(r.descripcion);
                fotoActual = r.fotoBase64;
                boolean esMio = r.usuarioId == SessionManager.get(FormularioRegistroActivity.this).getUsuarioId();
                lblInfo.setText(getString(R.string.info_registro,
                        esMio ? getString(R.string.autor_yo) : r.creadoPor,
                        Fechas.isoAFechaLocal(r.fechaCreacion)));
                lblInfo.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(ErrorApi error) {
                super.onError(error);
                finish();
            }

            @Override
            public void onTerminado() {
                cargando(false);
            }
        });
    }

    private void dictar() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.dicta_descripcion));
        try {
            dictado.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.sin_reconocimiento_voz, Toast.LENGTH_LONG).show();
        }
    }

    private void guardar() {
        capaTitulo.setError(null);
        capaDescripcion.setError(null);

        String titulo = txtTitulo.getText() == null ? "" : txtTitulo.getText().toString().trim();
        String descripcion = txtDescripcion.getText() == null ? "" : txtDescripcion.getText().toString().trim();

        boolean valido = true;
        if (titulo.length() < 3 || titulo.length() > 100) {
            capaTitulo.setError(getString(R.string.error_titulo));
            valido = false;
        }
        if (descripcion.isEmpty() || descripcion.length() > 500) {
            capaDescripcion.setError(getString(R.string.error_descripcion));
            valido = false;
        }
        if (!valido) {
            return;
        }

        RegistroRequest cuerpo = new RegistroRequest(titulo, descripcion, fotoActual);
        Call<RegistroDetalle> llamada = id == 0
                ? ApiClient.get(this).crear(cuerpo)
                : ApiClient.get(this).actualizar(id, cuerpo);

        cargando(true);
        llamada.enqueue(new ApiCallback<RegistroDetalle>(this) {
            @Override
            public void onExito(RegistroDetalle r) {
                Toast.makeText(FormularioRegistroActivity.this, R.string.registro_guardado, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(ErrorApi error) {
                String eTitulo = error.errorDe("Titulo");
                String eDescripcion = error.errorDe("Descripcion");
                if (eTitulo != null) capaTitulo.setError(eTitulo);
                if (eDescripcion != null) capaDescripcion.setError(eDescripcion);
                if (eTitulo == null && eDescripcion == null) super.onError(error);
            }

            // 403: el registro es de otro usuario. Se informa y la sesión sigue abierta.

            @Override
            public void onTerminado() {
                cargando(false);
            }
        });
    }

    private void cargando(boolean activo) {
        progreso.setVisibility(activo ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!activo);
    }
}
