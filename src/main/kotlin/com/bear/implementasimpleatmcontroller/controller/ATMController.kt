package com.bear.implementasimpleatmcontroller.controller

import jakarta.validation.Valid
import com.bear.implementasimpleatmcontroller.controller.atm.ATMRequest
import com.bear.implementasimpleatmcontroller.controller.atm.ATMResponse
import com.bear.implementasimpleatmcontroller.service.atm.ATMTransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
}