package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus

class InvalidTransactionAmountException(
    amount: Int,
    minAmount: Int? = null,
    maxAmount: Int? = null,
    reason: String? = null
) : BankException(
    status = HttpStatus.BAD_REQUEST,
    reason = reason ?: "Invalid transaction amount: $amount",
    errorCode = "INVALID_AMOUNT"
) {
    init {
        body.title = "Invalid Transaction Amount"
        body.setProperty("amount", amount)

        val constraints = mutableMapOf<String, Any>()
        minAmount?.let { constraints["minimum"] = it }
        maxAmount?.let { constraints["maximum"] = it }

        if (constraints.isNotEmpty()) {
            val message = when {
                minAmount != null && maxAmount != null -> "Amount must be between $minAmount and $maxAmount"
                minAmount != null -> "Amount must be at least $minAmount"
                maxAmount != null -> "Amount must not exceed $maxAmount"
                else -> "Invalid amount"
            }
            constraints["message"] = message
            body.setProperty("constraints", constraints)
        }
    }
}
