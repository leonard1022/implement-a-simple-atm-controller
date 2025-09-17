package kr.co.wanted.implementasimpleatmcontroller.service.atm.model

data class DepositResult(
    val previousBalance: Int,
    val depositedAmount: Int,
    val newBalance: Int
)