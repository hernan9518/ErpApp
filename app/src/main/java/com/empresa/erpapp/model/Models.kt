package com.empresa.erpapp.model

import com.google.gson.annotations.SerializedName

// ---------- Autenticación (JWT) ----------
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("accessToken") val token: String
)

data class PerfilResponse(
    @SerializedName("firstName") val nombre: String,
    @SerializedName("lastName") val apellido: String
)

// ---------- CRM: clientes ----------
data class EmpresaDto(
    @SerializedName("name") val nombre: String?
)

data class ClienteDto(
    val id: Int,
    @SerializedName("firstName") val nombre: String,
    @SerializedName("lastName") val apellido: String,
    val email: String,
    val company: EmpresaDto?
)

data class ClientesResponse(val users: List<ClienteDto>)

// ---------- ERP: catálogo y pedidos ----------
data class ProductoDto(
    val id: Int,
    @SerializedName("title") val nombre: String,
    @SerializedName("price") val precio: Double,
    val stock: Int
)

data class ProductosResponse(val products: List<ProductoDto>)

data class PedidoItemDto(
    val id: Int,
    @SerializedName("quantity") val cantidad: Int
)

data class CrearPedidoRequest(
    @SerializedName("userId") val clienteId: Int,
    @SerializedName("products") val lineas: List<PedidoItemDto>
)

data class ConfirmacionPedidoResponse(
    @SerializedName("id") val numeroOrden: Int,
    @SerializedName("total") val total: Double,
    @SerializedName("totalQuantity") val unidades: Int
)