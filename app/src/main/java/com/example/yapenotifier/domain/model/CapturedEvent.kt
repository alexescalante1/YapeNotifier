package com.example.yapenotifier.domain.model

data class CapturedEvent(
    val id: Long = 0,
    val amount: String,
    val time: String,
    val text: String,
    val timestamp: Long,
    val smsSent: Boolean = false,
    val packageName: String = ""
)
