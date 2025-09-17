package com.bear.implementasimpleatmcontroller.exception.base

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.time.Instant

abstract class BankException(
    val status: HttpStatus,
    reason: String,
    val errorCode: String,
    cause: Throwable? = null
) : ErrorResponseException(status, asProblemDetail(status, reason, errorCode), cause) {

    companion object {
        private fun asProblemDetail(
            status: HttpStatus,
            detail: String,
            errorCode: String
        ): ProblemDetail {
            val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
            problemDetail.setProperty("errorCode", errorCode)
            problemDetail.setProperty("timestamp", Instant.now())
            return problemDetail
        }
    }

    init {
        val problemDetail = body
        problemDetail.title = this::class.simpleName?.replace(Regex("([A-Z])"), " $1")?.trim()
    }
}
