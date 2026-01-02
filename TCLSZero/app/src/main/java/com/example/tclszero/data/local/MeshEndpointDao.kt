package com.example.tclszero.data.local



import androidx.room.*
import com.example.tclszero.domain.model.MeshEndpoint


@Dao
interface MeshEndpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(endpoint: MeshEndpoint)

    @Query("SELECT * FROM mesh_endpoints WHERE isConnected = 1")
    suspend fun getConnectedEndpoints(): List<MeshEndpoint>

    @Delete
    suspend fun deleteEndpoint(endpoint: MeshEndpoint)
}
