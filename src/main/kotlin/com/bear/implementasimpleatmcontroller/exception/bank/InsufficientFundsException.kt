package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus

class InsufficientFundsException(
    accountNumber: String,
    requestedAmount: Int,
    availableBalance: Int
) : BankException(
    status = HttpStatus.PAYMENT_REQUIRED,
    reason = "Insufficient funds in account. Requested: $requestedAmount, Available: $availableBalance",
    errorCode = "INSUFFICIENT_FUNDS"
) {
    init {
        body.title = "Insufficient Funds"
        body.setProperty("accountNumber", accountNumber)
        body.setProperty("requestedAmount", requestedAmount)
        body.setProperty("availableBalance", availableBalance)
        body.setProperty("shortfall", requestedAmount - availableBalance)
    }
}
