package com.empresa.erpapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {

    @Insert
    suspend fun insertar(pedido: PedidoEntity)

    @Query("SELECT * FROM pedidos ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<PedidoEntity>>
}