package com.example.yapenotifier.util

object Constants {
    const val DEDUP_WINDOW_MS = 30_000L
    const val MAX_EVENTS = 500
    const val NOTIFICATION_CHANNEL_ID = "yape_notifier_channel"
    const val NOTIFICATION_ID = 1001
    const val DEFAULT_YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"

    val YAPE_KEYWORDS = listOf(
        "te yapearon",
        "te han yapeado",
        "recibiste un yape",
        "recibiste dinero",
        "yape"
    )

    val AMOUNT_REGEX = Regex("(?i)(s/\\s*|s\\.\\s*|s/\\.\\s*)([0-9]+([.,][0-9]{1,2})?)")
    val TIME_REGEX = Regex("(?i)\\b([01]?\\d|2[0-3]):[0-5]\\d\\b")
    val PHONE_REGEX = Regex("^\\+?[0-9]{7,15}$")
}
