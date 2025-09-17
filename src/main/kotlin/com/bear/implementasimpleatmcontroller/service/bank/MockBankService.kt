package com.bear.implementasimpleatmcontroller.service.bank

import com.bear.implementasimpleatmcontroller.config.ATMConfiguration
import com.bear.implementasimpleatmcontroller.domain.Account
import com.bear.implementasimpleatmcontroller.domain.Card
import com.bear.implementasimpleatmcontroller.exception.base.BankException
import com.bear.implementasimpleatmcontroller.exception.bank.AccountNotFoundException
import com.bear.implementasimpleatmcontroller.exception.bank.BankSystemException
import com.bear.implementasimpleatmcontroller.exception.bank.CardNotFoundException
import com.bear.implementasimpleatmcontroller.exception.bank.DailyLimitExceededException
import com.bear.implementasimpleatmcontroller.exception.bank.InactiveCardException
import com.bear.implementasimpleatmcontroller.exception.bank.InsufficientFundsException
import com.bear.implementasimpleatmcontroller.exception.bank.InvalidTransactionAmountException
import com.bear.implementasimpleatmcontroller.repository.AccountRepository
import com.bear.implementasimpleatmcontroller.repository.CardRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MockBankService(
    private val cardRepository: CardRepository,
    private val accountRepository: AccountRepository,
    private val atmConfiguration: ATMConfiguration
) : BankService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun validatePin(cardNumber: String, pin: String): Boolean {
        return try {
            val card = cardRepository.findByCardNumber(cardNumber)
                ?: throw CardNotFoundException(cardNumber)

            if (!card.isActive) {
                throw InactiveCardException(cardNumber)
            }

            val isValid = card.pin == pin
            if (!isValid) {
                logger.warn("Invalid PIN attempt for card: $cardNumber")
            }

            isValid
        } catch (e: CardNotFoundException) {
            logger.error("Card validation failed: ${e.message}")
            throw e
        } catch (e: InactiveCardException) {
            logger.error("Inactive card access attempt: ${e.message}")
            throw e
        } catch (e: DataAccessException) {
            logger.error("Database error during PIN validation", e)
            throw BankSystemException("Unable to validate PIN", e)
        }
    }

    override fun getCardInfo(cardNumber: String): Card? {
        return try {
            cardRepository.findByCardNumber(cardNumber)?.also { card ->
                if (!card.isActive) {
                    logger.warn("Attempt to get info for inactive card: $cardNumber")
                }
            }
        } catch (e: DataAccessException) {
            logger.error("Database error getting card info", e)
            throw BankSystemException("Unable to retrieve card information", e)
        }
    }

    override fun getAccounts(cardNumber: String): List<Account> {
        return try {
            val card = cardRepository.findByCardNumber(cardNumber)
                ?: throw CardNotFoundException(cardNumber)

            if (!card.isActive) {
                throw InactiveCardException(cardNumber)
            }

            accountRepository.findByCard(card).also { accounts ->
                logger.debug("Retrieved ${accounts.size} accounts for card: $cardNumber")
            }
        } catch (e: CardNotFoundException) {
            logger.error("Card not found: ${e.message}")
            emptyList()
        } catch (e: InactiveCardException) {
            logger.warn("Inactive card: ${e.message}")
            emptyList()
        } catch (e: DataAccessException) {
            logger.error("Database error getting accounts", e)
            throw BankSystemException("Unable to retrieve accounts", e)
        }
    }

    override fun getBalance(accountNumber: String): Int {
        return try {
            val account = accountRepository.findByAccountNumber(accountNumber)
                ?: throw AccountNotFoundException(accountNumber)

            logger.debug("Balance query for account $accountNumber: ${account.balance}")
            account.balance
        } catch (e: AccountNotFoundException) {
            logger.error("Account not found: ${e.message}")
            throw e
        } catch (e: DataAccessException) {
            logger.error("Database error getting balance", e)
            throw BankSystemException("Unable to retrieve balance", e)
        }
    }

    override fun deposit(accountNumber: String, amount: Int): Boolean {
        return try {
            validateTransactionAmount(amount)

            val maxDeposit = atmConfiguration.transaction.maxSingleDeposit
            if (amount > maxDeposit) {
                throw InvalidTransactionAmountException(
                    amount = amount,
                    minAmount = atmConfiguration.transaction.minAmount,
                    maxAmount = maxDeposit,
                    reason = "Deposit amount exceeds maximum limit of $maxDeposit"
                )
            }

            val account = accountRepository.findByAccountNumber(accountNumber)
                ?: throw AccountNotFoundException(accountNumber)

            val dailyLimit = atmConfiguration.daily.maxDeposit
            val dailyTotal = calculateDailyTransactionTotal(accountNumber, "DEPOSIT")
            if (dailyTotal + amount > dailyLimit) {
                throw DailyLimitExceededException(accountNumber, dailyLimit, dailyTotal)
            }

            account.deposit(amount)
            accountRepository.save(account)

            logger.info("Deposit successful: $amount to account $accountNumber. New balance: ${account.balance}")
            true
        } catch (e: BankException) {
            logger.error("Deposit failed: ${e.message}")
            throw e
        } catch (e: DataAccessException) {
            logger.error("Database error during deposit", e)
            throw BankSystemException("Deposit transaction failed", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during deposit", e)
            false
        }
    }

    override fun withdraw(accountNumber: String, amount: Int): Boolean {
        return try {
            validateTransactionAmount(amount)

            val maxWithdrawal = atmConfiguration.transaction.maxSingleWithdrawal
            if (amount > maxWithdrawal) {
                throw InvalidTransactionAmountException(
                    amount = amount,
                    minAmount = atmConfiguration.transaction.minAmount,
                    maxAmount = maxWithdrawal,
                    reason = "Withdrawal amount exceeds maximum limit of $maxWithdrawal"
                )
            }

            val account = accountRepository.findByAccountNumber(accountNumber)
                ?: throw AccountNotFoundException(accountNumber)

            if (account.balance < amount) {
                throw InsufficientFundsException(accountNumber, amount, account.balance)
            }

            val dailyLimit = atmConfiguration.daily.maxWithdrawal
            val dailyTotal = calculateDailyTransactionTotal(accountNumber, "WITHDRAW")
            if (dailyTotal + amount > dailyLimit) {
                throw DailyLimitExceededException(accountNumber, dailyLimit, dailyTotal)
            }

            account.withdraw(amount)
            accountRepository.save(account)

            logger.info("Withdrawal successful: $amount from account $accountNumber. New balance: ${account.balance}")
            true
        } catch (e: BankException) {
            logger.error("Withdrawal failed: ${e.message}")
            throw e
        } catch (e: DataAccessException) {
            logger.error("Database error during withdrawal", e)
            throw BankSystemException("Withdrawal transaction failed", e)
        } catch (e: Exception) {
            logger.error("Unexpected error during withdrawal", e)
            false
        }
    }

    override fun blockCard(cardNumber: String): Boolean {
        return try {
            val card = cardRepository.findByCardNumber(cardNumber)
                ?: throw CardNotFoundException(cardNumber)

            if (!card.isActive) {
                logger.info("Card $cardNumber is already blocked")
                return true
            }

            card.isActive = false
            cardRepository.save(card)

            logger.warn("Card blocked: $cardNumber")
            true
        } catch (e: CardNotFoundException) {
            logger.error("Cannot block non-existent card: ${e.message}")
            false
        } catch (e: DataAccessException) {
            logger.error("Database error blocking card", e)
            throw BankSystemException("Unable to block card", e)
        }
    }

    private fun validateTransactionAmount(amount: Int) {
        val minAmount = atmConfiguration.transaction.minAmount
        if (amount < minAmount) {
            throw InvalidTransactionAmountException(
                amount = amount,
                minAmount = minAmount,
                reason = "Amount must be at least $minAmount"
            )
        }
    }

    private fun calculateDailyTransactionTotal(accountNumber: String, transactionType: String): Int {
        // TODO ::실제 구현에서는 Transaction 테이블에서 오늘 날짜의 거래 합계를 계산해야 함 (현재는 Mock 구현이므로 0 반환)
        return 0
    }
}
