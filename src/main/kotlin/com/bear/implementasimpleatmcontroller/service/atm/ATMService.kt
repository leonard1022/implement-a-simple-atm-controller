package com.bear.implementasimpleatmcontroller.service.atm

import com.bear.implementasimpleatmcontroller.service.atm.model.*

interface ATMService {
    fun insertCard(cardNumber: String): CardInsertionResult
    fun verifyPin(sessionId: String, pin: String): PinVerificationResult
    fun selectAccount(sessionId: String, accountNumber: String): AccountSelectionResult
    fun checkBalance(sessionId: String): BalanceResult
    fun deposit(sessionId: String, amount: Int): DepositResult
    fun withdraw(sessionId: String, amount: Int): WithdrawalResult
    fun endSession(sessionId: String): Boolean
}