package com.example.tclszero.data.local



import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tclszero.domain.model.CommandPost
import com.example.tclszero.domain.model.MeshEndpoint
import com.example.tclszero.domain.model.SoldierNode


@Database(
    entities = [SoldierNode::class, CommandPost::class, MeshEndpoint::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soldierNodeDao(): SoldierNodeDao
    abstract fun commandPostDao(): CommandPostDao
    abstract fun meshEndpointDao(): MeshEndpointDao
}
