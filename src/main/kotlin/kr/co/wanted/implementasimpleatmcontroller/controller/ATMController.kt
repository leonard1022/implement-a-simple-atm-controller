package kr.co.wanted.implementasimpleatmcontroller.controller

import jakarta.validation.Valid
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.ATMRequest
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.ATMResponse
import kr.co.wanted.implementasimpleatmcontroller.service.atm.ATMTransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/atm")
class ATMController(
    private val atmTransactionService: ATMTransactionService
) {

    @PostMapping("/transaction")
    fun performCompleteTransaction(
        @Valid @RequestBody request: ATMRequest
    ): ResponseEntity<ATMResponse> {
        val response = atmTransactionService.processTransaction(request)

        return when {
            response.success -> ResponseEntity.ok(response)
            response.errorCode == "INVALID_PIN" || response.errorCode == "CARD_BLOCKED" ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
            response.errorCode == "INVALID_STATE" ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(response)
            response.errorCode == "INTERNAL_ERROR" ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
            else -> ResponseEntity.badRequest().body(response)
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