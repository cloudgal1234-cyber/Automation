package com.automation.voicegesture.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AutomationDao {

    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun observeAll(): LiveData<List<Automation>>

    @Query("SELECT * FROM automations")
    suspend fun getAll(): List<Automation>

    @Insert
    suspend fun insert(automation: Automation): Long

    @Delete
    suspend fun delete(automation: Automation)
}
