package com.uth.bitacora.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.uth.bitacora.R;
import com.uth.bitacora.modelos.Modelos.RegistroResumen;
import com.uth.bitacora.util.Fechas;

import java.util.ArrayList;
import java.util.List;

/** Lista de registros. El botón Eliminar solo se muestra a los administradores. */
public class RegistrosAdapter extends RecyclerView.Adapter<RegistrosAdapter.Fila> {

    public interface Acciones {
        void editar(RegistroResumen registro);
        void eliminar(RegistroResumen registro);
    }

    private final List<RegistroResumen> datos = new ArrayList<>();
    private final Acciones acciones;
    private final boolean esAdmin;
    private final int miUsuarioId;

    public RegistrosAdapter(Acciones acciones, boolean esAdmin, int miUsuarioId) {
        this.acciones = acciones;
        this.esAdmin = esAdmin;
        this.miUsuarioId = miUsuarioId;
    }

    public void setDatos(List<RegistroResumen> nuevos) {
        datos.clear();
        if (nuevos != null) {
            datos.addAll(nuevos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View v = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_registro, padre, false);
        return new Fila(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        RegistroResumen r = datos.get(posicion);
        boolean esMio = r.usuarioId == miUsuarioId;

        fila.titulo.setText(r.titulo);
        fila.descripcion.setText(r.descripcion);
        String autor = esMio ? fila.itemView.getContext().getString(R.string.autor_yo) : r.creadoPor;
        fila.meta.setText(autor + " · " + Fechas.isoAFechaLocal(r.fechaCreacion) + (r.tieneFoto ? " · 📷" : ""));

        fila.btnEditar.setOnClickListener(v -> acciones.editar(r));
        fila.itemView.setOnClickListener(v -> acciones.editar(r));

        // Ocultar Eliminar a quien no es Admin. La API lo valida de todos modos (403).
        fila.btnEliminar.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
        fila.btnEliminar.setOnClickListener(v -> acciones.eliminar(r));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class Fila extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView descripcion;
        final TextView meta;
        final MaterialButton btnEditar;
        final MaterialButton btnEliminar;

        Fila(View v) {
            super(v);
            titulo = v.findViewById(R.id.lblTitulo);
            descripcion = v.findViewById(R.id.lblDescripcion);
            meta = v.findViewById(R.id.lblMeta);
            btnEditar = v.findViewById(R.id.btnEditar);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }
}
