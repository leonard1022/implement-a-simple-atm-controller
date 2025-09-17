package kr.co.wanted.implementasimpleatmcontroller.service.atm

import kr.co.wanted.implementasimpleatmcontroller.controller.atm.ATMRequest
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.ATMResponse
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.TransactionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ATMTransactionService(
    private val atmService: ATMService
) {

    fun processTransaction(request: ATMRequest): ATMResponse {
        var sessionId: String? = null

        return try {
            // Step 1: Insert Card
            val insertResult = atmService.insertCard(request.cardNumber)
            sessionId = insertResult.sessionId

            // Step 2: Verify PIN
            val pinResult = atmService.verifyPin(sessionId, request.pin)
            if (!pinResult.verified) {
                return createPinErrorResponse(request, pinResult.cardBlocked, pinResult.remainingAttempts)
            }

            // Step 3: Validate and Select Account
            val availableAccounts = pinResult.accounts
            val targetAccount = availableAccounts.find { it.accountNumber == request.accountNumber }

            if (targetAccount == null) {
                atmService.endSession(sessionId)
                return createAccountNotFoundResponse(request, availableAccounts.map { it.accountNumber })
            }

            atmService.selectAccount(sessionId, request.accountNumber)

            // Step 4: Perform Transaction
            val response = when (request.transactionType) {
                TransactionType.CHECK_BALANCE -> processBalanceInquiry(sessionId, targetAccount)
                TransactionType.DEPOSIT -> processDeposit(sessionId, request, targetAccount)
                TransactionType.WITHDRAW -> processWithdrawal(sessionId, request, targetAccount)
            }

            // Step 5: End Session
            atmService.endSession(sessionId)

            response

        } catch (e: IllegalArgumentException) {
            cleanupSession(sessionId)
            createErrorResponse(request, "BAD_REQUEST", e.message ?: "Invalid request")
        } catch (e: IllegalStateException) {
            cleanupSession(sessionId)
            createErrorResponse(request, "INVALID_STATE", e.message ?: "Invalid state")
        } catch (e: Exception) {
            cleanupSession(sessionId)
            createErrorResponse(request, "INTERNAL_ERROR", "Transaction failed: ${e.message}")
        }
    }

    private fun processBalanceInquiry(
        sessionId: String,
        targetAccount: kr.co.wanted.implementasimpleatmcontroller.domain.Account
    ): ATMResponse {
        val balanceResult = atmService.checkBalance(sessionId)
        return ATMResponse(
            success = true,
            message = "Balance inquiry successful",
            transactionType = TransactionType.CHECK_BALANCE.name,
            accountNumber = targetAccount.accountNumber,
            accountType = targetAccount.accountType.name,
            balance = balanceResult.balance
        )
    }

    private fun processDeposit(
        sessionId: String,
        request: ATMRequest,
        targetAccount: kr.co.wanted.implementasimpleatmcontroller.domain.Account
    ): ATMResponse {
        if (request.amount == null || request.amount <= 0) {
            atmService.endSession(sessionId)
            return createInvalidAmountResponse(request, "Deposit")
        }

        val depositResult = atmService.deposit(sessionId, request.amount)
        return ATMResponse(
            success = true,
            message = "Deposit successful",
            transactionType = TransactionType.DEPOSIT.name,
            accountNumber = targetAccount.accountNumber,
            accountType = targetAccount.accountType.name,
            previousBalance = depositResult.previousBalance,
            transactionAmount = depositResult.depositedAmount,
            newBalance = depositResult.newBalance
        )
    }

    private fun processWithdrawal(
        sessionId: String,
        request: ATMRequest,
        targetAccount: kr.co.wanted.implementasimpleatmcontroller.domain.Account
    ): ATMResponse {
        if (request.amount == null || request.amount <= 0) {
            atmService.endSession(sessionId)
            return createInvalidAmountResponse(request, "Withdrawal")
        }

        val withdrawResult = atmService.withdraw(sessionId, request.amount)
        return ATMResponse(
            success = true,
            message = "Withdrawal successful. Please take your cash.",
            transactionType = TransactionType.WITHDRAW.name,
            accountNumber = targetAccount.accountNumber,
            accountType = targetAccount.accountType.name,
            previousBalance = withdrawResult.previousBalance,
            transactionAmount = withdrawResult.withdrawnAmount,
            newBalance = withdrawResult.newBalance
        )
    }

    private fun createPinErrorResponse(
        request: ATMRequest,
        cardBlocked: Boolean,
        remainingAttempts: Int?
    ): ATMResponse {
        return ATMResponse(
            success = false,
            message = if (cardBlocked) "Card has been blocked"
                     else "Invalid PIN. ${remainingAttempts} attempts remaining",
            transactionType = request.transactionType.name,
            errorCode = if (cardBlocked) "CARD_BLOCKED" else "INVALID_PIN",
            errorDetails = "PIN verification failed"
        )
    }

    private fun createAccountNotFoundResponse(
        request: ATMRequest,
        availableAccounts: List<String>
    ): ATMResponse {
        return ATMResponse(
            success = false,
            message = "Account ${request.accountNumber} not found. Available accounts: ${
                availableAccounts.joinToString()
            }",
            transactionType = request.transactionType.name,
            errorCode = "ACCOUNT_NOT_FOUND",
            errorDetails = "The specified account does not exist for this card"
        )
    }

    private fun createInvalidAmountResponse(
        request: ATMRequest,
        operation: String
    ): ATMResponse {
        return ATMResponse(
            success = false,
            message = "$operation amount is required and must be positive",
            transactionType = request.transactionType.name,
            errorCode = "INVALID_AMOUNT",
            errorDetails = "Amount must be greater than 0"
        )
    }

    private fun createErrorResponse(
        request: ATMRequest,
        errorCode: String,
        message: String
    ): ATMResponse {
        return ATMResponse(
            success = false,
            message = message,
            transactionType = request.transactionType.name,
            errorCode = errorCode,
            errorDetails = message
        )
    }

    private fun cleanupSession(sessionId: String?) {
        sessionId?.let {
            try {
                atmService.endSession(it)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}