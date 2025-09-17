package kr.co.wanted.implementasimpleatmcontroller.controller.atm

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class CompleteATMRequest(
    @field:NotBlank(message = "Card number is required")
    @field:Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
    val cardNumber: String,

    @field:NotBlank(message = "PIN is required")
    @field:Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4-6 digits")
    val pin: String,

    @field:NotBlank(message = "Account number is required")
    val accountNumber: String,

    @field:NotNull(message = "Transaction type is required")
    val transactionType: TransactionType,

    // For DEPOSIT/WITHDRAW operations
    @field:Min(value = 1, message = "Amount must be at least 1")
    val amount: Int? = null
)

enum class TransactionType {
    CHECK_BALANCE,
    DEPOSIT,
    WITHDRAW
}
