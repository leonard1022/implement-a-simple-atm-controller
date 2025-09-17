package com.bear.implementasimpleatmcontroller.service.atm.model

data class CardInsertionResult(
    val sessionId: String,
    val requiresPin: Boolean = true
)