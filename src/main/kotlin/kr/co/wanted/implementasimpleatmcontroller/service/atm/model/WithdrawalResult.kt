package kr.co.wanted.implementasimpleatmcontroller.service.atm.model

data class WithdrawalResult(
    val previousBalance: Int,
    val withdrawnAmount: Int,
    val newBalance: Int,
    val dispensed: Boolean = true
)