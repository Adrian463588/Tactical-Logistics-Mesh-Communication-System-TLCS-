package com.example.tclszero.data.mesh



import com.example.tclszero.domain.model.SoldierNode
import com.google.gson.Gson

import timber.log.Timber

data class CoordinatePayload(
    val type: String = "COORD",
    val nodeId: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val heartRate: Int,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class InventoryPayload(
    val type: String = "INVENTORY",
    val postId: String,
    val ammo: Int,
    val meds: Int,
    val rations: Int,
    val timestamp: Long = System.currentTimeMillis()
)

object PayloadSerializer {
    private val gson = Gson()

    fun serializeCoordinate(node: SoldierNode): ByteArray {
        val payload = CoordinatePayload(
            nodeId = node.nodeId,
            displayName = node.displayName,
            latitude = node.latitude,
            longitude = node.longitude,
            heartRate = node.heartRate,
            status = node.status
        )
        return gson.toJson(payload).toByteArray()
    }

    fun deserializeCoordinate(data: ByteArray): CoordinatePayload? {
        return try {
            gson.fromJson(String(data), CoordinatePayload::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Failed to deserialize coordinate")
            null
        }
    }

    fun serializeInventory(postId: String, ammo: Int, meds: Int, rations: Int): ByteArray {
        val payload = InventoryPayload(
            postId = postId,
            ammo = ammo,
            meds = meds,
            rations = rations
        )
        return gson.toJson(payload).toByteArray()
    }

    fun deserializeInventory(data: ByteArray): InventoryPayload? {
        return try {
            gson.fromJson(String(data), InventoryPayload::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Failed to deserialize inventory")
            null
        }
    }
}
