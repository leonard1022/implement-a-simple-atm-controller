package com.bear.implementasimpleatmcontroller.exception.bank

import com.bear.implementasimpleatmcontroller.exception.base.BankException
import org.springframework.http.HttpStatus

class CardNotFoundException(
    cardNumber: String
) : BankException(
    status = HttpStatus.NOT_FOUND,
    reason = "Card not found with number: $cardNumber",
    errorCode = "CARD_NOT_FOUND"
) {
    init {
        body.setProperty("cardNumber", maskCardNumber(cardNumber))
    }

    private fun maskCardNumber(cardNumber: String): String {
        return if (cardNumber.length > 8) {
            "${cardNumber.take(4)}****${cardNumber.takeLast(4)}"
        } else {
            "****"
        }
    }
}
