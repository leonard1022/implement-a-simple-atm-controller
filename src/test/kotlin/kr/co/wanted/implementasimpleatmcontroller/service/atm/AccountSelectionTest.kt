package kr.co.wanted.implementasimpleatmcontroller.service.atm

import kr.co.wanted.implementasimpleatmcontroller.domain.*
import kr.co.wanted.implementasimpleatmcontroller.repository.ATMSessionRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.TransactionRepository
import kr.co.wanted.implementasimpleatmcontroller.service.bank.BankService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class AccountSelectionTest {

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

    private val checkingAccount = Account(
        accountNumber = "CHK001",
        accountType = AccountType.CHECKING,
        balance = 1000,
        card = testCard
    )

    private val savingsAccount = Account(
        accountNumber = "SAV001",
        accountType = AccountType.SAVINGS,
        balance = 5000,
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
    fun `selectAccount should successfully select checking account`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.PIN_VERIFIED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getAccounts(testCard.cardNumber))
            .thenReturn(listOf(checkingAccount, savingsAccount))
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }

        // When
        val result = atmService.selectAccount(sessionId, "CHK001")

        // Then
        assertEquals(checkingAccount, result.account)
        assertEquals("CHK001", result.account.accountNumber)
        assertEquals(AccountType.CHECKING, result.account.accountType)
        assertEquals(SessionStatus.ACCOUNT_SELECTED, session.sessionStatus)
        assertEquals(checkingAccount, session.selectedAccount)
        verify(sessionRepository).save(any<ATMSession>())
    }

    @Test
    fun `selectAccount should successfully select savings account`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.PIN_VERIFIED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getAccounts(testCard.cardNumber))
            .thenReturn(listOf(checkingAccount, savingsAccount))
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }

        // When
        val result = atmService.selectAccount(sessionId, "SAV001")

        // Then
        assertEquals(savingsAccount, result.account)
        assertEquals("SAV001", result.account.accountNumber)
        assertEquals(AccountType.SAVINGS, result.account.accountType)
        assertEquals(SessionStatus.ACCOUNT_SELECTED, session.sessionStatus)
        assertEquals(savingsAccount, session.selectedAccount)
    }

    @Test
    fun `selectAccount should throw exception for invalid account number`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.PIN_VERIFIED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getAccounts(testCard.cardNumber))
            .thenReturn(listOf(checkingAccount, savingsAccount))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            atmService.selectAccount(sessionId, "INVALID001")
        }
        assertEquals("Invalid account number", exception.message)
        assertEquals(SessionStatus.PIN_VERIFIED, session.sessionStatus) // Status should not change
        assertNull(session.selectedAccount) // No account should be selected
    }

    @Test
    fun `selectAccount should throw exception for invalid session`() {
        // Given
        whenever(sessionRepository.findBySessionIdAndSessionStatusNot("invalid-session", SessionStatus.CLOSED))
            .thenReturn(null)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            atmService.selectAccount("invalid-session", "ACC001")
        }
        assertEquals("Invalid session", exception.message)
    }

    @Test
    fun `selectAccount should throw exception when PIN not verified`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.CARD_INSERTED // Wrong status - should be PIN_VERIFIED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.selectAccount(sessionId, "ACC001")
        }
        assertEquals("PIN not verified", exception.message)
    }

    @Test
    fun `selectAccount should throw exception when no card in session`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = null, // No card
            sessionStatus = SessionStatus.PIN_VERIFIED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.selectAccount(sessionId, "ACC001")
        }
        assertEquals("No card in session", exception.message)
    }

    @Test
    fun `should not allow selecting account when already selected`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED, // Already selected
            selectedAccount = checkingAccount
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.selectAccount(sessionId, "SAV001")
        }
        assertEquals("PIN not verified", exception.message) // Because status is not PIN_VERIFIED
    }

    @Test
    fun `selectAccount should work with single account`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.PIN_VERIFIED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.getAccounts(testCard.cardNumber))
            .thenReturn(listOf(checkingAccount)) // Only one account
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }

        // When
        val result = atmService.selectAccount(sessionId, "CHK001")

        // Then
        assertEquals(checkingAccount, result.account)
        assertEquals(SessionStatus.ACCOUNT_SELECTED, session.sessionStatus)
    }
}