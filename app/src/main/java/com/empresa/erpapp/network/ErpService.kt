package com.empresa.erpapp.network

import com.empresa.erpapp.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ErpService {

    @POST("auth/login")
    suspend fun login(@Body credenciales: LoginRequest): Response<LoginResponse>

    // Requiere el encabezado Authorization: Bearer <token>
    @GET("auth/me")
    suspend fun perfil(): Response<PerfilResponse>

    // "CRM": cartera de clientes
    @GET("users")
    suspend fun listarClientes(
        @Query("limit") limite: Int,
        @Query("skip") salto: Int
    ): Response<ClientesResponse>

    // "ERP": catálogo de productos
    @GET("products")
    suspend fun listarProductos(@Query("limit") limite: Int): Response<ProductosResponse>

    // "ERP": registrar pedido (la API lo simula, no persiste)
    @POST("carts/add")
    suspend fun registrarPedido(@Body pedido: CrearPedidoRequest): Response<ConfirmacionPedidoResponse>
}