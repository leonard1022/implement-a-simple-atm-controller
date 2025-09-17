package com.bear.implementasimpleatmcontroller.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.bear.implementasimpleatmcontroller.controller.atm.ATMRequest
import com.bear.implementasimpleatmcontroller.controller.atm.ATMResponse
import com.bear.implementasimpleatmcontroller.controller.atm.TransactionType
import com.bear.implementasimpleatmcontroller.service.atm.ATMTransactionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(ATMController::class)
class ATMControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var atmTransactionService: ATMTransactionService

    @Test
    fun `successful balance check returns 200`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        val response = ATMResponse(
            success = true,
            message = "Balance inquiry successful",
            transactionType = "CHECK_BALANCE",
            accountNumber = "ACC001",
            accountType = "CHECKING",
            balance = 1000
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Balance inquiry successful"))
            .andExpect(jsonPath("$.balance").value(1000))
    }

    @Test
    fun `successful deposit returns 200`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.DEPOSIT,
            amount = 500
        )

        val response = ATMResponse(
            success = true,
            message = "Deposit successful",
            transactionType = "DEPOSIT",
            accountNumber = "ACC001",
            accountType = "CHECKING",
            previousBalance = 1000,
            transactionAmount = 500,
            newBalance = 1500
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.newBalance").value(1500))
    }

    @Test
    fun `successful withdrawal returns 200`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.WITHDRAW,
            amount = 200
        )

        val response = ATMResponse(
            success = true,
            message = "Withdrawal successful. Please take your cash.",
            transactionType = "WITHDRAW",
            accountNumber = "ACC001",
            accountType = "CHECKING",
            previousBalance = 1000,
            transactionAmount = 200,
            newBalance = 800
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.newBalance").value(800))
    }

    @Test
    fun `invalid PIN returns 401`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "9999",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        val response = ATMResponse(
            success = false,
            message = "Invalid PIN. 2 attempts remaining",
            transactionType = "CHECK_BALANCE",
            errorCode = "INVALID_PIN",
            errorDetails = "PIN verification failed"
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_PIN"))
    }

    @Test
    fun `card blocked returns 401`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "9999",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        val response = ATMResponse(
            success = false,
            message = "Card has been blocked",
            transactionType = "CHECK_BALANCE",
            errorCode = "CARD_BLOCKED",
            errorDetails = "PIN verification failed"
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("CARD_BLOCKED"))
    }

    @Test
    fun `invalid amount returns 400`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.WITHDRAW,
            amount = null  // null amount for withdrawal should return 400
        )

        val response = ATMResponse(
            success = false,
            message = "Amount must be positive",
            transactionType = "WITHDRAW",
            errorCode = "BAD_REQUEST",
            errorDetails = "Invalid amount"
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
    }

    @Test
    fun `invalid state returns 409`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        val response = ATMResponse(
            success = false,
            message = "Invalid session state",
            transactionType = "CHECK_BALANCE",
            errorCode = "INVALID_STATE",
            errorDetails = "Session error"
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_STATE"))
    }

    @Test
    fun `internal error returns 500`() {
        val request = ATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        val response = ATMResponse(
            success = false,
            message = "System error occurred",
            transactionType = "CHECK_BALANCE",
            errorCode = "INTERNAL_ERROR",
            errorDetails = "Database connection failed"
        )

        whenever(atmTransactionService.processTransaction(any())).thenReturn(response)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
    }
}