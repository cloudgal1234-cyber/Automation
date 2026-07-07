package com.automation.voicegesture.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined link between a spoken trigger word and a custom on-screen gesture.
 * [strokesJson] stores the recorded [GestureStroke] list serialized as JSON (see [Converters]).
 */
@Entity(tableName = "automations")
data class Automation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerWord: String,
    val strokesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
