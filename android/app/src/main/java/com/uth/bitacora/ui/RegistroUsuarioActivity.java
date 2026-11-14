package com.uth.bitacora.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.uth.bitacora.R;
import com.uth.bitacora.api.ApiCallback;
import com.uth.bitacora.api.ApiClient;
import com.uth.bitacora.api.ErrorApi;
import com.uth.bitacora.modelos.Modelos.RegistroUsuarioRequest;
import com.uth.bitacora.modelos.Modelos.UsuarioDto;

/** Crear una cuenta (siempre con el rol Usuario). */
public class RegistroUsuarioActivity extends AppCompatActivity {

    private TextInputLayout capaCorreo;
    private TextInputLayout capaContrasena;
    private TextInputLayout capaConfirmar;
    private TextInputEditText txtCorreo;
    private TextInputEditText txtContrasena;
    private TextInputEditText txtConfirmar;
    private MaterialButton btnCrear;
    private ProgressBar progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro_usuario);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        capaCorreo = findViewById(R.id.capaCorreo);
        capaContrasena = findViewById(R.id.capaContrasena);
        capaConfirmar = findViewById(R.id.capaConfirmar);
        txtCorreo = findViewById(R.id.txtCorreo);
        txtContrasena = findViewById(R.id.txtContrasena);
        txtConfirmar = findViewById(R.id.txtConfirmar);
        btnCrear = findViewById(R.id.btnCrear);
        progreso = findViewById(R.id.progreso);

        btnCrear.setOnClickListener(v -> registrar());
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    private void registrar() {
        capaCorreo.setError(null);
        capaContrasena.setError(null);
        capaConfirmar.setError(null);

        String correo = texto(txtCorreo);
        String contrasena = texto(txtContrasena);
        String confirmar = texto(txtConfirmar);

        // Mismas reglas que la API: correo válido; contraseña de 8+ caracteres con letra y número.
        boolean valido = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            capaCorreo.setError(getString(R.string.error_correo));
            valido = false;
        }
        if (contrasena.length() < 8 || !contrasena.matches(".*[A-Za-z].*") || !contrasena.matches(".*\\d.*")) {
            capaContrasena.setError(getString(R.string.error_contrasena_regla));
            valido = false;
        }
        if (!contrasena.equals(confirmar)) {
            capaConfirmar.setError(getString(R.string.error_contrasena_no_coincide));
            valido = false;
        }
        if (!valido) {
            return;
        }

        cargando(true);
        ApiClient.get(this).registrar(new RegistroUsuarioRequest(correo, contrasena)).enqueue(new ApiCallback<UsuarioDto>(this) {
            @Override
            public void onExito(UsuarioDto usuario) {
                Toast.makeText(RegistroUsuarioActivity.this, R.string.cuenta_creada, Toast.LENGTH_LONG).show();
                startActivity(new Intent(RegistroUsuarioActivity.this, LoginActivity.class)
                        .putExtra(LoginActivity.EXTRA_CORREO, usuario.correo)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            }

            @Override
            public void onError(ErrorApi error) {
                String errCorreo = error.errorDe("Correo");
                String errContrasena = error.errorDe("Contrasena");
                if (errCorreo != null) capaCorreo.setError(errCorreo);
                if (errContrasena != null) capaContrasena.setError(errContrasena);
                if (errCorreo == null && errContrasena == null) super.onError(error);
            }

            @Override
            public void onTerminado() {
                cargando(false);
            }
        });
    }

    private void cargando(boolean activo) {
        progreso.setVisibility(activo ? View.VISIBLE : View.GONE);
        btnCrear.setEnabled(!activo);
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
