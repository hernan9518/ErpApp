package com.empresa.erpapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empresa.erpapp.data.AppDatabase
import com.empresa.erpapp.data.PedidoEntity
import com.empresa.erpapp.model.*
import com.empresa.erpapp.network.ApiClient
import com.empresa.erpapp.network.ErpRepository
import com.empresa.erpapp.network.Resultado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val cargando: Boolean = false,
    val usuario: String? = null,
    val clientes: List<ClienteDto> = emptyList(),
    val productos: List<ProductoDto> = emptyList(),
    val clienteSeleccionado: ClienteDto? = null,
    val cantidades: Map<Int, Int> = emptyMap(),
    val confirmacion: ConfirmacionPedidoResponse? = null,
    val mensaje: String? = null
)

class VentasViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ErpRepository()
    private val dao = AppDatabase.obtener(application).pedidoDao()

    private val _estado = MutableStateFlow(UiState())
    val estado: StateFlow<UiState> = _estado.asStateFlow()

    // Historial local (Room): se actualiza solo cada vez que se inserta un pedido
    val historial: StateFlow<List<PedidoEntity>> = dao.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun iniciarSesion(usuario: String, clave: String) {
        if (usuario.isBlank() || clave.isBlank()) {
            _estado.update { it.copy(mensaje = "Ingresa usuario y contraseña") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensaje = null) }

            when (val login = repo.login(usuario.trim(), clave)) {
                is Resultado.Fallo ->
                    _estado.update { it.copy(cargando = false, mensaje = "No se pudo iniciar sesión: ${login.mensaje}") }
                is Resultado.Exito -> {
                    ApiClient.token = login.datos.token
                    // /auth/me exige el Bearer token: confirma que el JWT funciona
                    when (val perfil = repo.perfil()) {
                        is Resultado.Fallo -> {
                            ApiClient.token = null
                            _estado.update { it.copy(cargando = false, mensaje = perfil.mensaje) }
                        }
                        is Resultado.Exito -> cargarDatos("${perfil.datos.nombre} ${perfil.datos.apellido}")
                    }
                }
            }
        }
    }

    private suspend fun cargarDatos(nombreUsuario: String) {
        val clientes = repo.clientes()
        val productos = repo.productos()
        if (clientes is Resultado.Exito && productos is Resultado.Exito) {
            _estado.update {
                it.copy(
                    cargando = false,
                    usuario = nombreUsuario,
                    clientes = clientes.datos.users,
                    productos = productos.datos.products
                )
            }
        } else {
            ApiClient.token = null
            val error = (clientes as? Resultado.Fallo)?.mensaje
                ?: (productos as? Resultado.Fallo)?.mensaje
                ?: "Error al cargar datos"
            _estado.update { it.copy(cargando = false, mensaje = error) }
        }
    }

    fun seleccionarCliente(cliente: ClienteDto) {
        _estado.update { it.copy(clienteSeleccionado = cliente, confirmacion = null, mensaje = null) }
    }

    fun cambiarCantidad(producto: ProductoDto, delta: Int) {
        _estado.update { actual ->
            val nueva = ((actual.cantidades[producto.id] ?: 0) + delta).coerceIn(0, producto.stock)
            actual.copy(cantidades = actual.cantidades + (producto.id to nueva), confirmacion = null, mensaje = null)
        }
    }

    fun enviarPedido() {
        val actual = _estado.value
        val cliente = actual.clienteSeleccionado
        val lineas = actual.cantidades.filter { it.value > 0 }.map { PedidoItemDto(it.key, it.value) }

        if (cliente == null) {
            _estado.update { it.copy(mensaje = "Selecciona un cliente") }
            return
        }
        if (lineas.isEmpty()) {
            _estado.update { it.copy(mensaje = "Agrega al menos un producto") }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, mensaje = null) }
            when (val r = repo.enviarPedido(CrearPedidoRequest(cliente.id, lineas))) {
                is Resultado.Exito -> {
                    val guardado = guardarEnHistorial(actual, cliente, lineas, r.datos)
                    _estado.update {
                        it.copy(
                            cargando = false,
                            confirmacion = r.datos,
                            cantidades = emptyMap(),
                            mensaje = if (guardado) null else "Pedido enviado, pero no se pudo guardar en el historial"
                        )
                    }
                }
                is Resultado.Fallo -> _estado.update { it.copy(cargando = false, mensaje = r.mensaje) }
            }
        }
    }

    private suspend fun guardarEnHistorial(
        estado: UiState,
        cliente: ClienteDto,
        lineas: List<PedidoItemDto>,
        confirmacion: ConfirmacionPedidoResponse
    ): Boolean = try {
        val resumen = lineas.joinToString { l ->
            val nombre = estado.productos.firstOrNull { it.id == l.id }?.nombre ?: "Producto ${l.id}"
            "$nombre x${l.cantidad}"
        }
        dao.insertar(
            PedidoEntity(
                numeroOrden = confirmacion.numeroOrden,
                clienteId = cliente.id,
                clienteNombre = "${cliente.nombre} ${cliente.apellido}",
                ejecutivo = estado.usuario ?: "",
                unidades = confirmacion.unidades,
                total = confirmacion.total,
                resumen = resumen,
                fecha = System.currentTimeMillis()
            )
        )
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    fun cerrarSesion() {
        ApiClient.token = null
        _estado.value = UiState()
    }
}