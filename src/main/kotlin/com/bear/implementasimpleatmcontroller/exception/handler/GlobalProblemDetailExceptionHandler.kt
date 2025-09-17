package com.bear.implementasimpleatmcontroller.exception.handler

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Instant

@RestControllerAdvice
class GlobalProblemDetailExceptionHandler : ResponseEntityExceptionHandler() {

    companion object {
        private val log = LoggerFactory.getLogger(GlobalProblemDetailExceptionHandler::class.java)
    }

    // Validation exceptions are handled by ResponseEntityExceptionHandler parent class

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ProblemDetail {
        log.error("Invalid argument: ${ex.message}")

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.message ?: "Invalid request parameters"
        )
        problemDetail.title = "Invalid Request"
        problemDetail.setProperty("errorCode", "BAD_REQUEST")
        problemDetail.setProperty("timestamp", Instant.now())

        return problemDetail
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ProblemDetail {
        log.error("Invalid state: ${ex.message}")

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.message ?: "The operation cannot be performed in the current state"
        )
        problemDetail.title = "Invalid State"
        problemDetail.setProperty("errorCode", "INVALID_STATE")
        problemDetail.setProperty("timestamp", Instant.now())

        return problemDetail
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ProblemDetail {
        log.error("Unexpected error occurred", ex)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please contact support if the problem persists."
        )
        problemDetail.title = "Internal Server Error"
        problemDetail.setProperty("errorCode", "INTERNAL_ERROR")
        problemDetail.setProperty("timestamp", Instant.now())

        return problemDetail
    }
}
