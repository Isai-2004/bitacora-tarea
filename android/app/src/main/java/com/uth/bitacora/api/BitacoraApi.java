package com.uth.bitacora.api;

import com.uth.bitacora.modelos.Modelos.LoginRequest;
import com.uth.bitacora.modelos.Modelos.LoginResponse;
import com.uth.bitacora.modelos.Modelos.RegistroDetalle;
import com.uth.bitacora.modelos.Modelos.RegistroRequest;
import com.uth.bitacora.modelos.Modelos.RegistroResumen;
import com.uth.bitacora.modelos.Modelos.RegistroUsuarioRequest;
import com.uth.bitacora.modelos.Modelos.UsuarioDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Url;

/** Endpoints de la Bitácora API. El token lo agrega AuthInterceptor. */
public interface BitacoraApi {

    @POST("api/auth/registro")
    Call<UsuarioDto> registrar(@Body RegistroUsuarioRequest cuerpo);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest cuerpo);

    @GET("api/registros")
    Call<List<RegistroResumen>> listar();

    @GET("api/registros/{id}")
    Call<RegistroDetalle> obtener(@Path("id") int id);

    @POST("api/registros")
    Call<RegistroDetalle> crear(@Body RegistroRequest cuerpo);

    @PUT("api/registros/{id}")
    Call<RegistroDetalle> actualizar(@Path("id") int id, @Body RegistroRequest cuerpo);

    @DELETE("api/registros/{id}")
    Call<Void> eliminar(@Path("id") int id);

    /**
     * Para el laboratorio de comparativa: misma consulta de listado, con la ruta del escenario
     * y el encabezado Authorization enviado explícitamente (null = no se envía).
     */
    @GET
    Call<List<RegistroResumen>> listarLaboratorio(@Url String ruta, @Header("Authorization") String autorizacion);
}
