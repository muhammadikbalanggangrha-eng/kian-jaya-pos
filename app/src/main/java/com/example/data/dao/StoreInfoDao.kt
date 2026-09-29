package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StoreInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreInfoDao {
    @Query("SELECT * FROM store_info WHERE id = 1 LIMIT 1")
    fun getStoreInfo(): Flow<StoreInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStoreInfo(storeInfo: StoreInfo)
}
