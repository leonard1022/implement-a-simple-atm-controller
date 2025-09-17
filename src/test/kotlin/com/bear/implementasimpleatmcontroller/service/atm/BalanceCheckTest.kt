package com.bear.implementasimpleatmcontroller.service.atm

import com.bear.implementasimpleatmcontroller.domain.*
import com.bear.implementasimpleatmcontroller.repository.ATMSessionRepository
import com.bear.implementasimpleatmcontroller.repository.TransactionRepository
import com.bear.implementasimpleatmcontroller.service.bank.BankService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class BalanceCheckTest {

    private lateinit var bankService: BankService
    private lateinit var sessionRepository: ATMSessionRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var atmService: ATMServiceImpl

    private val testCard = Card(
        cardNumber = "1234567890123456",
        pin = "1234",
        cardHolderName = "Test User",
        isActive = true
    )

    private val testAccount = Account(
        accountNumber = "ACC001",
        accountType = AccountType.CHECKING,
        balance = 2500,
        card = testCard
    )

    @BeforeEach
    fun setUp() {
        bankService = mock()
        sessionRepository = mock()
        transactionRepository = mock()
        atmService = ATMServiceImpl(bankService, sessionRepository, transactionRepository)
    }

    @Test
    fun `checkBalance should return current balance for selected account`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            selectedAccount = testAccount,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getBalance(testAccount.accountNumber))
            .thenReturn(2500)
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer { it.arguments[0] as Transaction }

        // When
        val result = atmService.checkBalance(sessionId)

        // Then
        assertEquals(testAccount.accountNumber, result.accountNumber)
        assertEquals(2500, result.balance)

        // Verify transaction was recorded
        verify(transactionRepository).save(argThat<Transaction> {
            transactionType == TransactionType.BALANCE_INQUIRY &&
            amount == 0 &&
            balanceAfter == 2500 &&
            account == testAccount
        })
    }

    @Test
    fun `checkBalance should get latest balance from bank service`() {
        // Given
        val sessionId = "test-session-id"
        val accountWithOldBalance = Account(
            accountNumber = "ACC001",
            accountType = AccountType.CHECKING,
            balance = 1000, // Old balance in session
            card = testCard
        )

        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            selectedAccount = accountWithOldBalance,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getBalance("ACC001"))
            .thenReturn(1500) // New balance from bank
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer { it.arguments[0] as Transaction }

        // When
        val result = atmService.checkBalance(sessionId)

        // Then
        assertEquals(1500, result.balance) // Should return latest balance
        verify(bankService).getBalance("ACC001")
    }

    @Test
    fun `checkBalance should throw exception for invalid session`() {
        // Given
        whenever(sessionRepository.findBySessionIdAndSessionStatusNot("invalid", SessionStatus.CLOSED))
            .thenReturn(null)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            atmService.checkBalance("invalid")
        }
        assertEquals("Invalid session", exception.message)
    }

    @Test
    fun `checkBalance should throw exception when no account selected`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.PIN_VERIFIED, // Not ACCOUNT_SELECTED
            selectedAccount = null
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.checkBalance(sessionId)
        }
        assertEquals("No account selected", exception.message)
    }

    @Test
    fun `checkBalance should throw exception when session has no account`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED,
            selectedAccount = null // No account despite correct status
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.checkBalance(sessionId)
        }
        assertEquals("No account in session", exception.message)
    }

    @Test
    fun `checkBalance should record transaction with zero amount`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            selectedAccount = testAccount,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getBalance(testAccount.accountNumber))
            .thenReturn(3000)
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer { it.arguments[0] as Transaction }

        // When
        atmService.checkBalance(sessionId)

        // Then
        verify(transactionRepository).save(argThat<Transaction> {
            transactionType == TransactionType.BALANCE_INQUIRY &&
            amount == 0 && // Balance inquiry has 0 amount
            balanceAfter == 3000
        })
    }

    @Test
    fun `multiple balance checks should each create a transaction`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            selectedAccount = testAccount,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getBalance(testAccount.accountNumber))
            .thenReturn(2500, 2600, 2700) // Different balances for each check
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer { it.arguments[0] as Transaction }

        // When - Check balance 3 times
        val result1 = atmService.checkBalance(sessionId)
        val result2 = atmService.checkBalance(sessionId)
        val result3 = atmService.checkBalance(sessionId)

        // Then
        assertEquals(2500, result1.balance)
        assertEquals(2600, result2.balance)
        assertEquals(2700, result3.balance)

        // Verify 3 transactions were created
        verify(transactionRepository, times(3)).save(any<Transaction>())
    }

    @Test
    fun `checkBalance should work for different account types`() {
        // Given - Savings account
        val savingsAccount = Account(
            accountNumber = "SAV001",
            accountType = AccountType.SAVINGS,
            balance = 10000,
            card = testCard
        )

        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            selectedAccount = savingsAccount,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getBalance("SAV001"))
            .thenReturn(10000)
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer { it.arguments[0] as Transaction }

        // When
        val result = atmService.checkBalance(sessionId)

        // Then
        assertEquals("SAV001", result.accountNumber)
        assertEquals(10000, result.balance)
    }
}