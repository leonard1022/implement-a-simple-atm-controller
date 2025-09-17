package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus

class InactiveCardException(
    cardNumber: String
) : BankException(
    status = HttpStatus.FORBIDDEN,
    reason = "Card is inactive or has been blocked",
    errorCode = "CARD_BLOCKED"
) {
    init {
        body.title = "Card Blocked"
        body.setProperty("cardNumber", maskCardNumber(cardNumber))
        body.setProperty("action", "Please contact customer service to reactivate your card")
    }

    private fun maskCardNumber(cardNumber: String): String {
        return if (cardNumber.length > 8) {
            "${cardNumber.take(4)}****${cardNumber.takeLast(4)}"
        } else {
            "****"
        }
    }
}