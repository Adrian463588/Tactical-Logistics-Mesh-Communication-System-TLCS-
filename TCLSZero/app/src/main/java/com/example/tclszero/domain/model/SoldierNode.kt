package com.example.tclszero.domain.model


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "soldier_nodes")
data class SoldierNode(
    @PrimaryKey
    val nodeId: String = UUID.randomUUID().toString(),
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val heartRate: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, OFFLINE, COMPROMISED
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val ammoCount: Int = 0,
    val medsCount: Int = 0,
    val rationCount: Int = 0
)

@Entity(tableName = "mesh_endpoints")
data class MeshEndpoint(
    @PrimaryKey
    val endpointId: String,
    val endpointName: String,
    val isConnected: Boolean = false,
    val connectedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_posts")
data class CommandPost(
    @PrimaryKey
    val postId: String = UUID.randomUUID().toString(),
    val postName: String,
    val latitude: Double,
    val longitude: Double,
    val ammoCapacity: Int,
    val medsCapacity: Int,
    val rationCapacity: Int
)
