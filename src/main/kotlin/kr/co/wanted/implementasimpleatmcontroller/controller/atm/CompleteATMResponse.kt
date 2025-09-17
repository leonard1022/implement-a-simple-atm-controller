package kr.co.wanted.implementasimpleatmcontroller.controller.atm

data class CompleteATMResponse(
    val success: Boolean,
    val message: String,

    // Transaction result
    val transactionType: String,
    val accountNumber: String? = null,
    val accountType: String? = null,

    // Balance information
    val balance: Int? = null,
    val previousBalance: Int? = null,
    val newBalance: Int? = null,
    val transactionAmount: Int? = null,

    // Error information
    val errorCode: String? = null,
    val errorDetails: String? = null
)
