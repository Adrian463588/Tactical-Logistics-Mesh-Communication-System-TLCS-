package com.example.tclszero.data.local


import androidx.room.*
import com.example.tclszero.domain.model.CommandPost

import kotlinx.coroutines.flow.Flow

@Dao
interface CommandPostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(post: CommandPost)

    @Query("SELECT * FROM command_posts")
    fun getAllPosts(): Flow<List<CommandPost>>

    @Delete
    suspend fun deletePost(post: CommandPost)

    @Query("UPDATE command_posts SET ammoCapacity = :ammo WHERE postId = :postId")
    suspend fun updateAmmo(postId: String, ammo: Int)

    @Query("DELETE FROM command_posts WHERE postId = :postId")
    suspend fun deleteById(postId: String)
}
