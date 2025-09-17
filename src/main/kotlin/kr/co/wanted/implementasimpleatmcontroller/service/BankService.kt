package kr.co.wanted.implementasimpleatmcontroller.service

import kr.co.wanted.implementasimpleatmcontroller.domain.Account
import kr.co.wanted.implementasimpleatmcontroller.domain.Card

interface BankService {
    fun validatePin(cardNumber: String, pin: String): Boolean
    fun getCardInfo(cardNumber: String): Card?
    fun getAccounts(cardNumber: String): List<Account>
    fun getBalance(accountNumber: String): Int
    fun deposit(accountNumber: String, amount: Int): Boolean
    fun withdraw(accountNumber: String, amount: Int): Boolean
    fun blockCard(cardNumber: String): Boolean
}