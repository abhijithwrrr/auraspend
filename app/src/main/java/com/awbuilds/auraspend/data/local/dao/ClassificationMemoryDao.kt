package com.awbuilds.auraspend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.awbuilds.auraspend.data.local.entities.ClassificationMemoryEntity

@Dao
interface ClassificationMemoryDao {

    @Query("SELECT * FROM classification_memory WHERE memoryKey = :key LIMIT 1")
    suspend fun getByKey(key: String): ClassificationMemoryEntity?

    @Query("SELECT * FROM classification_memory ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ClassificationMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClassificationMemoryEntity)

    @Query("DELETE FROM classification_memory WHERE memoryKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM classification_memory")
    suspend fun clear()
}
