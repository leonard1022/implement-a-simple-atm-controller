package kr.co.wanted.implementasimpleatmcontroller.controller

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.wanted.implementasimpleatmcontroller.domain.Account
import kr.co.wanted.implementasimpleatmcontroller.domain.AccountType
import kr.co.wanted.implementasimpleatmcontroller.domain.Card
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.CompleteATMRequest
import kr.co.wanted.implementasimpleatmcontroller.controller.atm.TransactionType
import kr.co.wanted.implementasimpleatmcontroller.service.atm.ATMService
import kr.co.wanted.implementasimpleatmcontroller.service.atm.model.*
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
    private lateinit var atmService: ATMService

    private val mockCard = Card(
        cardNumber = "1234567890123456",
        pin = "1234",
        cardHolderName = "Test User",
        isActive = true
    )

    @Test
    fun `complete balance check transaction in single call`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "1234")).thenReturn(
            PinVerificationResult(
                verified = true,
                accounts = listOf(
                    Account(
                        accountNumber = "ACC001",
                        accountType = AccountType.CHECKING,
                        balance = 1000,
                        card = mockCard
                    )
                ),
                remainingAttempts = null,
                cardBlocked = false
            )
        )

        whenever(atmService.selectAccount("session-123", "ACC001")).thenReturn(
            AccountSelectionResult(
                account = Account(
                    accountNumber = "ACC001",
                    accountType = AccountType.CHECKING,
                    balance = 1000,
                    card = mockCard
                )
            )
        )

        whenever(atmService.checkBalance("session-123")).thenReturn(
            BalanceResult(accountNumber = "ACC001", balance = 1000)
        )

        whenever(atmService.endSession("session-123")).thenReturn(true)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Balance inquiry successful"))
            .andExpect(jsonPath("$.transactionType").value("CHECK_BALANCE"))
            .andExpect(jsonPath("$.accountNumber").value("ACC001"))
            .andExpect(jsonPath("$.accountType").value("CHECKING"))
            .andExpect(jsonPath("$.balance").value(1000))

        val inOrder = inOrder(atmService)
        inOrder.verify(atmService).insertCard("1234567890123456")
        inOrder.verify(atmService).verifyPin("session-123", "1234")
        inOrder.verify(atmService).selectAccount("session-123", "ACC001")
        inOrder.verify(atmService).checkBalance("session-123")
        inOrder.verify(atmService).endSession("session-123")
    }

    @Test
    fun `complete withdrawal transaction in single call`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.WITHDRAW,
            amount = 200
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "1234")).thenReturn(
            PinVerificationResult(
                verified = true,
                accounts = listOf(
                    Account(
                        accountNumber = "ACC001",
                        accountType = AccountType.CHECKING,
                        balance = 1000,
                        card = mockCard
                    )
                ),
                remainingAttempts = null,
                cardBlocked = false
            )
        )

        whenever(atmService.selectAccount("session-123", "ACC001")).thenReturn(
            AccountSelectionResult(
                account = Account(
                    accountNumber = "ACC001",
                    accountType = AccountType.CHECKING,
                    balance = 1000,
                    card = mockCard
                )
            )
        )

        whenever(atmService.withdraw("session-123", 200)).thenReturn(
            WithdrawalResult(
                previousBalance = 1000,
                withdrawnAmount = 200,
                newBalance = 800
            )
        )

        whenever(atmService.endSession("session-123")).thenReturn(true)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Withdrawal successful. Please take your cash."))
            .andExpect(jsonPath("$.transactionType").value("WITHDRAW"))
            .andExpect(jsonPath("$.previousBalance").value(1000))
            .andExpect(jsonPath("$.transactionAmount").value(200))
            .andExpect(jsonPath("$.newBalance").value(800))

        verify(atmService).insertCard("1234567890123456")
        verify(atmService).verifyPin("session-123", "1234")
        verify(atmService).selectAccount("session-123", "ACC001")
        verify(atmService).withdraw("session-123", 200)
        verify(atmService).endSession("session-123")
    }

    @Test
    fun `complete deposit transaction in single call`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC002",
            transactionType = TransactionType.DEPOSIT,
            amount = 500
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "1234")).thenReturn(
            PinVerificationResult(
                verified = true,
                accounts = listOf(
                    Account(
                        accountNumber = "ACC002",
                        accountType = AccountType.SAVINGS,
                        balance = 5000,
                        card = mockCard
                    )
                ),
                remainingAttempts = null,
                cardBlocked = false
            )
        )

        whenever(atmService.selectAccount("session-123", "ACC002")).thenReturn(
            AccountSelectionResult(
                account = Account(
                    accountNumber = "ACC002",
                    accountType = AccountType.SAVINGS,
                    balance = 5000,
                    card = mockCard
                )
            )
        )

        whenever(atmService.deposit("session-123", 500)).thenReturn(
            DepositResult(
                previousBalance = 5000,
                depositedAmount = 500,
                newBalance = 5500
            )
        )

        whenever(atmService.endSession("session-123")).thenReturn(true)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Deposit successful"))
            .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
            .andExpect(jsonPath("$.accountType").value("SAVINGS"))
            .andExpect(jsonPath("$.previousBalance").value(5000))
            .andExpect(jsonPath("$.transactionAmount").value(500))
            .andExpect(jsonPath("$.newBalance").value(5500))
    }

    @Test
    fun `fail transaction with incorrect PIN`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "9999",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "9999")).thenReturn(
            PinVerificationResult(
                verified = false,
                accounts = emptyList(),
                remainingAttempts = 2,
                cardBlocked = false
            )
        )

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid PIN. 2 attempts remaining"))
            .andExpect(jsonPath("$.errorCode").value("INVALID_PIN"))

        // Verify session was not continued after PIN failure
        verify(atmService).insertCard("1234567890123456")
        verify(atmService).verifyPin("session-123", "9999")
        verify(atmService, never()).selectAccount(any(), any())
    }

    @Test
    fun `fail transaction when card is blocked`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "9999",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "9999")).thenReturn(
            PinVerificationResult(
                verified = false,
                accounts = emptyList(),
                remainingAttempts = 0,
                cardBlocked = true
            )
        )

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Card has been blocked"))
            .andExpect(jsonPath("$.errorCode").value("CARD_BLOCKED"))
    }

    @Test
    fun `fail transaction when account not found`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "WRONG_ACCOUNT",
            transactionType = TransactionType.CHECK_BALANCE
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "1234")).thenReturn(
            PinVerificationResult(
                verified = true,
                accounts = listOf(
                    Account(
                        accountNumber = "ACC001",
                        accountType = AccountType.CHECKING,
                        balance = 1000,
                        card = mockCard
                    )
                ),
                remainingAttempts = null,
                cardBlocked = false
            )
        )

        whenever(atmService.endSession("session-123")).thenReturn(true)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Account WRONG_ACCOUNT not found. Available accounts: ACC001"))
            .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))

        verify(atmService).endSession("session-123")
    }

    @Test
    fun `fail withdrawal without amount`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.WITHDRAW,
            amount = null
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "1234")).thenReturn(
            PinVerificationResult(
                verified = true,
                accounts = listOf(
                    Account(
                        accountNumber = "ACC001",
                        accountType = AccountType.CHECKING,
                        balance = 1000,
                        card = mockCard
                    )
                ),
                remainingAttempts = null,
                cardBlocked = false
            )
        )

        whenever(atmService.selectAccount("session-123", "ACC001")).thenReturn(
            AccountSelectionResult(
                account = Account(
                    accountNumber = "ACC001",
                    accountType = AccountType.CHECKING,
                    balance = 1000,
                    card = mockCard
                )
            )
        )

        whenever(atmService.endSession("session-123")).thenReturn(true)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Withdrawal amount is required and must be positive"))
            .andExpect(jsonPath("$.errorCode").value("INVALID_AMOUNT"))

        verify(atmService).endSession("session-123")
    }

    @Test
    fun `cleanup session when exception occurs`() {
        val request = CompleteATMRequest(
            cardNumber = "1234567890123456",
            pin = "1234",
            accountNumber = "ACC001",
            transactionType = TransactionType.CHECK_BALANCE
        )

        whenever(atmService.insertCard("1234567890123456")).thenReturn(
            CardInsertionResult(sessionId = "session-123", requiresPin = true)
        )

        whenever(atmService.verifyPin("session-123", "1234"))
            .thenThrow(RuntimeException("System error"))

        whenever(atmService.endSession("session-123")).thenReturn(true)

        mockMvc.perform(
            post("/atm/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))

        verify(atmService).endSession("session-123")
    }
}
