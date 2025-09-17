package kr.co.wanted.implementasimpleatmcontroller.service.atm.model

import kr.co.wanted.implementasimpleatmcontroller.domain.Account

data class PinVerificationResult(
    val verified: Boolean,
    val accounts: List<Account> = emptyList(),
    val remainingAttempts: Int? = null,
    val cardBlocked: Boolean = false
)