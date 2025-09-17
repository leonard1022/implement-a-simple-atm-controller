package kr.co.wanted.implementasimpleatmcontroller.controller

import jakarta.validation.Valid
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.ATMRequest
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.ATMResponse
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.TransactionType
import kr.co.wanted.implementasimpleatmcontroller.service.atm.ATMService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/atm")
class ATMController(
    private val atmService: ATMService
) {

    @PostMapping("/transaction")
    fun performCompleteTransaction(
        @Valid @RequestBody request: ATMRequest
    ): ResponseEntity<ATMResponse> {

        var sessionId: String? = null

        return try {
            // Step 1: Insert Card
            val insertResult = atmService.insertCard(request.cardNumber)
            sessionId = insertResult.sessionId

            // Step 2: Verify PIN
            val pinResult = atmService.verifyPin(sessionId, request.pin)
            if (!pinResult.verified) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ATMResponse(
                        success = false,
                        message = if (pinResult.cardBlocked) "Card has been blocked"
                                 else "Invalid PIN. ${pinResult.remainingAttempts} attempts remaining",
                        transactionType = request.transactionType.name,
                        errorCode = if (pinResult.cardBlocked) "CARD_BLOCKED" else "INVALID_PIN",
                        errorDetails = "PIN verification failed"
                    )
                )
            }

            // Step 3: Validate and Select Account
            val availableAccounts = pinResult.accounts
            val targetAccount = availableAccounts.find { it.accountNumber == request.accountNumber }

            if (targetAccount == null) {
                atmService.endSession(sessionId)
                return ResponseEntity.badRequest().body(
                    ATMResponse(
                        success = false,
                        message = "Account ${request.accountNumber} not found. Available accounts: ${
                            availableAccounts.joinToString { it.accountNumber }
                        }",
                        transactionType = request.transactionType.name,
                        errorCode = "ACCOUNT_NOT_FOUND",
                        errorDetails = "The specified account does not exist for this card"
                    )
                )
            }

            atmService.selectAccount(sessionId, request.accountNumber)

            // Step 4: Perform Transaction
            val response = when (request.transactionType) {
                TransactionType.CHECK_BALANCE -> {
                    val balanceResult = atmService.checkBalance(sessionId)
                    ATMResponse(
                        success = true,
                        message = "Balance inquiry successful",
                        transactionType = TransactionType.CHECK_BALANCE.name,
                        accountNumber = targetAccount.accountNumber,
                        accountType = targetAccount.accountType.name,
                        balance = balanceResult.balance
                    )
                }

                TransactionType.DEPOSIT -> {
                    if (request.amount == null || request.amount <= 0) {
                        atmService.endSession(sessionId)
                        return ResponseEntity.badRequest().body(
                            ATMResponse(
                                success = false,
                                message = "Deposit amount is required and must be positive",
                                transactionType = TransactionType.DEPOSIT.name,
                                errorCode = "INVALID_AMOUNT",
                                errorDetails = "Amount must be greater than 0"
                            )
                        )
                    }

                    val depositResult = atmService.deposit(sessionId, request.amount)
                    ATMResponse(
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

                TransactionType.WITHDRAW -> {
                    if (request.amount == null || request.amount <= 0) {
                        atmService.endSession(sessionId)
                        return ResponseEntity.badRequest().body(
                            ATMResponse(
                                success = false,
                                message = "Withdrawal amount is required and must be positive",
                                transactionType = TransactionType.WITHDRAW.name,
                                errorCode = "INVALID_AMOUNT",
                                errorDetails = "Amount must be greater than 0"
                            )
                        )
                    }

                    val withdrawResult = atmService.withdraw(sessionId, request.amount)
                    ATMResponse(
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
            }

            // Step 5: End Session
            atmService.endSession(sessionId)

            ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            // Clean up session if it exists
            sessionId?.let {
                try { atmService.endSession(it) } catch (_: Exception) {}
            }

            ResponseEntity.badRequest().body(
                ATMResponse(
                    success = false,
                    message = e.message ?: "Invalid request",
                    transactionType = request.transactionType.name,
                    errorCode = "BAD_REQUEST",
                    errorDetails = e.message
                )
            )
        } catch (e: IllegalStateException) {
            // Clean up session if it exists
            sessionId?.let {
                try { atmService.endSession(it) } catch (_: Exception) {}
            }

            ResponseEntity.status(HttpStatus.CONFLICT).body(
                ATMResponse(
                    success = false,
                    message = e.message ?: "Invalid state",
                    transactionType = request.transactionType.name,
                    errorCode = "INVALID_STATE",
                    errorDetails = e.message
                )
            )
        } catch (e: Exception) {
            // Clean up session if it exists
            sessionId?.let {
                try { atmService.endSession(it) } catch (_: Exception) {}
            }

            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ATMResponse(
                    success = false,
                    message = "Transaction failed: ${e.message}",
                    transactionType = request.transactionType.name,
                    errorCode = "INTERNAL_ERROR",
                    errorDetails = e.message
                )
            )
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ATMResponse> {
        val errors = ex.bindingResult.allErrors
            .mapNotNull { error ->
                when (error) {
                    is FieldError -> "${error.field}: ${error.defaultMessage}"
                    else -> error.defaultMessage
                }
            }
            .joinToString(", ")

        return ResponseEntity.badRequest().body(
            ATMResponse(
                success = false,
                message = "Validation failed: $errors",
                transactionType = "UNKNOWN",
                errorCode = "VALIDATION_ERROR",
                errorDetails = errors
            )
        )
    }
}
