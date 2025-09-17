package kr.co.wanted.implementasimpleatmcontroller.service.atm.model

data class CardInsertionResult(
    val sessionId: String,
    val requiresPin: Boolean = true
)