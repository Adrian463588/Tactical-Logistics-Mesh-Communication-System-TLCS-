package com.example.tclszero.data.local

import androidx.room.*
import com.example.tclszero.domain.model.SoldierNode

import kotlinx.coroutines.flow.Flow

@Dao
interface SoldierNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(node: SoldierNode)

    @Query("SELECT * FROM soldier_nodes")
    fun getAllNodes(): Flow<List<SoldierNode>>

    @Query("SELECT * FROM soldier_nodes WHERE nodeId = :nodeId")
    suspend fun getNodeById(nodeId: String): SoldierNode?

    @Delete
    suspend fun deleteNode(node: SoldierNode)

    @Query("UPDATE soldier_nodes SET latitude = :lat, longitude = :lon WHERE nodeId = :nodeId")
    suspend fun updatePosition(nodeId: String, lat: Double, lon: Double)

    @Query("UPDATE soldier_nodes SET heartRate = :hr WHERE nodeId = :nodeId")
    suspend fun updateHeartRate(nodeId: String, hr: Int)

    @Query("UPDATE soldier_nodes SET status = :status WHERE nodeId = :nodeId")
    suspend fun updateStatus(nodeId: String, status: String)

    @Query("DELETE FROM soldier_nodes WHERE nodeId = :nodeId")
    suspend fun deleteById(nodeId: String)
}
