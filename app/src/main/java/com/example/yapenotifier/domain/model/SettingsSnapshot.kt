package com.example.yapenotifier.domain.model

data class SettingsSnapshot(
    val packages: Set<String>,
    val captureAll: Boolean,
    val numbers: Set<String>
)
