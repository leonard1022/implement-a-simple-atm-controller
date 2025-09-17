package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus
import java.util.UUID

class BankSystemException(
    message: String,
    cause: Throwable? = null
) : BankException(
    status = HttpStatus.SERVICE_UNAVAILABLE,
    reason = "Bank system error: $message",
    errorCode = "BANK_SYSTEM_ERROR",
    cause = cause
) {
    init {
        body.title = "System Temporarily Unavailable"
        body.setProperty("message", "The banking system is temporarily unavailable. Please try again later.")
    }
}
