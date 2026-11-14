package com.uth.bitacora.laboratorio;

/** Contrato común de los tres clientes HTTP del laboratorio. */
public interface ClienteLaboratorio {

    /** La respuesta SIEMPRE se entrega en el hilo principal (para actualizar la interfaz). */
    interface AlTerminar {
        void resultado(Resultado resultado);
    }

    String nombre();

    void listar(Laboratorio.Escenario escenario, AlTerminar alTerminar);

    /** Cancela las peticiones en curso (al salir de la pantalla). */
    void cancelar();
}
