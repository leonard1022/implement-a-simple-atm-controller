package com.bear.implementasimpleatmcontroller.service.atm.model

import com.bear.implementasimpleatmcontroller.domain.Account

data class PinVerificationResult(
    val verified: Boolean,
    val accounts: List<Account> = emptyList(),
    val remainingAttempts: Int? = null,
    val cardBlocked: Boolean = false
)