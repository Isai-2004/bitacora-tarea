package com.uth.bitacora.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.uth.bitacora.R;
import com.uth.bitacora.api.ApiCallback;
import com.uth.bitacora.api.ApiClient;
import com.uth.bitacora.api.ErrorApi;
import com.uth.bitacora.modelos.Modelos.LoginRequest;
import com.uth.bitacora.modelos.Modelos.LoginResponse;
import com.uth.bitacora.sesion.SessionManager;

/** Inicio de sesión. Si ya hay una sesión vigente, entra directo a la bitácora. */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_CORREO = "correo";

    private TextInputLayout capaCorreo;
    private TextInputLayout capaContrasena;
    private TextInputEditText txtCorreo;
    private TextInputEditText txtContrasena;
    private TextView lblMensaje;
    private MaterialButton btnEntrar;
    private ProgressBar progreso;
    private SessionManager sesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sesion = SessionManager.get(this);

        String mensaje = getIntent().getStringExtra(Navegacion.EXTRA_MENSAJE);
        if (mensaje == null && sesion.estaVigente()) {
            abrirBitacora();
            return;
        }
        if (mensaje == null && sesion.getToken() != null) {
            sesion.cerrar();
            mensaje = getString(R.string.sesion_vencida);
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        Pantalla.ajustarBordes(findViewById(R.id.raiz));

        capaCorreo = findViewById(R.id.capaCorreo);
        capaContrasena = findViewById(R.id.capaContrasena);
        txtCorreo = findViewById(R.id.txtCorreo);
        txtContrasena = findViewById(R.id.txtContrasena);
        lblMensaje = findViewById(R.id.lblMensaje);
        btnEntrar = findViewById(R.id.btnEntrar);
        progreso = findViewById(R.id.progreso);

        mostrarMensaje(mensaje);
        String correoPrevio = getIntent().getStringExtra(EXTRA_CORREO);
        if (correoPrevio != null) {
            txtCorreo.setText(correoPrevio);
        }
        btnEntrar.setOnClickListener(v -> iniciarSesion());
        findViewById(R.id.btnIrRegistro).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroUsuarioActivity.class)));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String correo = intent.getStringExtra(EXTRA_CORREO);
        if (correo != null && txtCorreo != null) {
            txtCorreo.setText(correo);
            txtContrasena.requestFocus();
        }
    }

    private void iniciarSesion() {
        capaCorreo.setError(null);
        capaContrasena.setError(null);
        mostrarMensaje(null);

        String correo = texto(txtCorreo);
        String contrasena = texto(txtContrasena);

        // Validación en el cliente antes de llamar a la API.
        boolean valido = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            capaCorreo.setError(getString(R.string.error_correo));
            valido = false;
        }
        if (TextUtils.isEmpty(contrasena)) {
            capaContrasena.setError(getString(R.string.error_contrasena_vacia));
            valido = false;
        }
        if (!valido) {
            return;
        }

        cargando(true);
        ApiClient.get(this).login(new LoginRequest(correo, contrasena)).enqueue(new ApiCallback<LoginResponse>(this) {
            @Override
            public void onExito(LoginResponse respuesta) {
                sesion.guardar(respuesta);
                abrirBitacora();
            }

            @Override
            public void onNoAutenticado(ErrorApi error) {
                // La API no dice si falló el correo o la contraseña.
                mostrarMensaje(error.mensaje());
                txtContrasena.setText("");
            }

            @Override
            public void onError(ErrorApi error) {
                mostrarMensaje(error.mensaje());
            }

            @Override
            public void onTerminado() {
                cargando(false);
            }
        });
    }

    private void abrirBitacora() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void cargando(boolean activo) {
        progreso.setVisibility(activo ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!activo);
    }

    private void mostrarMensaje(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisibility(TextUtils.isEmpty(mensaje) ? View.GONE : View.VISIBLE);
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
