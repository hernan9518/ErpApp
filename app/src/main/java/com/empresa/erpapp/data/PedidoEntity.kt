package com.empresa.erpapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedidos")
data class PedidoEntity(
    // id propio: el número de orden de DummyJSON puede repetirse (es simulado)
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numeroOrden: Int,
    val clienteId: Int,
    val clienteNombre: String,
    val ejecutivo: String,
    val unidades: Int,
    val total: Double,
    val resumen: String,
    val fecha: Long
)