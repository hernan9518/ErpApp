package com.empresa.erpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.empresa.erpapp.data.PedidoEntity
import com.empresa.erpapp.model.ClienteDto
import com.empresa.erpapp.ui.UiState
import com.empresa.erpapp.ui.VentasViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) { AppRoot() }
            }
        }
    }
}

@Composable
fun AppRoot(vm: VentasViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    val historial by vm.historial.collectAsState()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    if (estado.usuario == null) {
        LoginScreen(estado, vm::iniciarSesion)
    } else {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Nuevo pedido") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Historial (${historial.size})") })
            }
            Box(Modifier.weight(1f)) {
                if (pestana == 0) {
                    PedidoScreen(estado, vm)
                } else {
                    HistorialScreen(historial, estado.clienteSeleccionado)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(estado: UiState, onLogin: (String, String) -> Unit) {
    // Credenciales de PRUEBA públicas de DummyJSON (no son datos reales)
    var usuario by remember { mutableStateOf("emilys") }
    var clave by remember { mutableStateOf("emilyspass") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("App Empresarial ERP / CRM", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onLogin(usuario, clave) }, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
            Text(if (estado.cargando) "Conectando..." else "Iniciar sesión")
        }
        estado.mensaje?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PedidoScreen(estado: UiState, vm: VentasViewModel) {
    val total = estado.productos.sumOf { (estado.cantidades[it.id] ?: 0) * it.precio }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ejecutivo: ${estado.usuario}", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = vm::cerrarSesion) { Text("Salir") }
            }
        }
        if (estado.cargando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

        item { Text("1. Cliente (CRM)", style = MaterialTheme.typography.titleLarge) }
        items(estado.clientes, key = { "c${it.id}" }) { c ->
            val seleccionado = c.id == estado.clienteSeleccionado?.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable { vm.seleccionarCliente(c) },
                colors = CardDefaults.cardColors(
                    containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("${c.nombre} ${c.apellido}", style = MaterialTheme.typography.titleSmall)
                    Text("${c.company?.nombre ?: "Sin empresa"} · ${c.email}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Text("2. Productos (ERP)", style = MaterialTheme.typography.titleLarge) }
        items(estado.productos, key = { "p${it.id}" }) { p ->
            val cantidad = estado.cantidades[p.id] ?: 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text("$${"%.2f".format(p.precio)} · stock ${p.stock}", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { vm.cambiarCantidad(p, -1) }, enabled = cantidad > 0) { Text("-") }
                Text("$cantidad", Modifier.padding(horizontal = 12.dp))
                OutlinedButton(onClick = { vm.cambiarCantidad(p, 1) }, enabled = cantidad < p.stock) { Text("+") }
            }
        }

        item {
            Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::enviarPedido, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar pedido al ERP")
            }
        }
        estado.mensaje?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        estado.confirmacion?.let { c ->
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Pedido registrado", style = MaterialTheme.typography.titleMedium)
                        Text("Orden #${c.numeroOrden} · ${c.unidades} unidades · Total $${"%.2f".format(c.total)}")
                    }
                }
            }
        }
    }
}

@Composable
fun HistorialScreen(pedidos: List<PedidoEntity>, clienteActual: ClienteDto?) {
    var soloCliente by rememberSaveable { mutableStateOf(false) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val lista = if (soloCliente && clienteActual != null) {
        pedidos.filter { it.clienteId == clienteActual.id }
    } else {
        pedidos
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historial de pedidos", style = MaterialTheme.typography.titleLarge)
        Text("Guardado en el dispositivo (Room)", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = soloCliente && clienteActual != null,
                onCheckedChange = { soloCliente = it },
                enabled = clienteActual != null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (clienteActual != null) "Solo ${clienteActual.nombre} ${clienteActual.apellido}"
                else "Selecciona un cliente para filtrar"
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${lista.size} pedidos · Total $${"%.2f".format(lista.sumOf { it.total })}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        if (lista.isEmpty()) {
            Text("Aún no hay pedidos registrados.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(lista, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Orden #${p.numeroOrden} · ${p.clienteNombre}", style = MaterialTheme.typography.titleSmall)
                            Text(formato.format(Date(p.fecha)) + " · Ejecutivo: ${p.ejecutivo}", style = MaterialTheme.typography.bodySmall)
                            Text(p.resumen, style = MaterialTheme.typography.bodyMedium)
                            Text("${p.unidades} unidades · Total $${"%.2f".format(p.total)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}