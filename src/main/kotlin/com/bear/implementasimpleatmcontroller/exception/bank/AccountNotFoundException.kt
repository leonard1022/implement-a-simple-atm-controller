package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus

class AccountNotFoundException(
    accountNumber: String
) : BankException(
    status = HttpStatus.NOT_FOUND,
    reason = "Account not found with number: $accountNumber",
    errorCode = "ACCOUNT_NOT_FOUND"
) {
    init {
        body.setProperty("accountNumber", accountNumber)
    }
}
