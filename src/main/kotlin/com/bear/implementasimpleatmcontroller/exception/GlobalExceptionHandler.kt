package com.bear.implementasimpleatmcontroller.exception

import com.bear.implementasimpleatmcontroller.controller.atm.ATMResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ATMResponse> {
        return ResponseEntity.badRequest().body(
            ATMResponse(
                success = false,
                message = ex.message ?: "Invalid request",
                transactionType = "UNKNOWN",
                errorCode = "BAD_REQUEST",
                errorDetails = ex.message
            )
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ATMResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ATMResponse(
                success = false,
                message = ex.message ?: "Invalid state",
                transactionType = "UNKNOWN",
                errorCode = "INVALID_STATE",
                errorDetails = ex.message
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ATMResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ATMResponse(
                success = false,
                message = "An unexpected error occurred",
                transactionType = "UNKNOWN",
                errorCode = "INTERNAL_ERROR",
                errorDetails = ex.message
            )
        )
    }
}