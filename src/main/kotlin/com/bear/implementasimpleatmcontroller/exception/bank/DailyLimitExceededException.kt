package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus
import java.time.LocalTime

class DailyLimitExceededException(
    accountNumber: String,
    dailyLimit: Int,
    currentTotal: Int
) : BankException(
    status = HttpStatus.TOO_MANY_REQUESTS,
    reason = "Daily transaction limit exceeded for account $accountNumber",
    errorCode = "DAILY_LIMIT_EXCEEDED"
) {
    init {
        body.title = "Daily Limit Exceeded"
        body.setProperty("accountNumber", accountNumber)
        body.setProperty("dailyLimit", dailyLimit)
        body.setProperty("currentTotal", currentTotal)
        body.setProperty("availableAmount", if (dailyLimit > currentTotal) dailyLimit - currentTotal else 0)
        body.setProperty("resetTime", LocalTime.of(0, 0).toString())
        body.setProperty("message", "Your daily transaction limit has been reached. The limit resets at midnight.")
    }
}
