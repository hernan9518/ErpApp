package com.empresa.erpapp.network

import com.empresa.erpapp.model.*
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

sealed class Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>()
    data class Fallo(val mensaje: String) : Resultado<Nothing>()
}

class ErpRepository(private val api: ErpService = ApiClient.erpService) {

    suspend fun login(usuario: String, clave: String) =
        llamar { api.login(LoginRequest(usuario, clave)) }

    suspend fun perfil() = llamar { api.perfil() }

    suspend fun clientes() = llamar { api.listarClientes(limite = 8, salto = 0) }

    suspend fun productos() = llamar { api.listarProductos(limite = 8) }

    suspend fun enviarPedido(pedido: CrearPedidoRequest) =
        llamar { api.registrarPedido(pedido) }

    // Retrofit ya ejecuta las funciones suspend fuera del hilo principal.
    private suspend fun <T> llamar(bloque: suspend () -> Response<T>): Resultado<T> =
        try {
            val respuesta = bloque()
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) {
                Resultado.Exito(cuerpo)
            } else {
                Resultado.Fallo("El servidor respondió con código ${respuesta.code()}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Resultado.Fallo("Sin conexión con el servidor. Revisa tu Internet.")
        } catch (e: Exception) {
            Resultado.Fallo("Error inesperado: ${e.message}")
        }
}