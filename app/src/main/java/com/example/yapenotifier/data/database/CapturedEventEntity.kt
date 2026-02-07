package com.example.yapenotifier.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.yapenotifier.domain.model.CapturedEvent

@Entity(tableName = "events")
data class CapturedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: String,
    val time: String,
    val text: String,
    val timestamp: Long,
    val smsSent: Boolean,
    val packageName: String
)

fun CapturedEventEntity.toDomain() = CapturedEvent(
    id = id,
    amount = amount,
    time = time,
    text = text,
    timestamp = timestamp,
    smsSent = smsSent,
    packageName = packageName
)

fun CapturedEvent.toEntity() = CapturedEventEntity(
    id = id,
    amount = amount,
    time = time,
    text = text,
    timestamp = timestamp,
    smsSent = smsSent,
    packageName = packageName
)
