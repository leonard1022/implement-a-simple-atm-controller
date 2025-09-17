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
import java.util.*

class ATMServiceTest {

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
        balance = 1000,
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
    fun `insertCard should create session for valid active card`() {
        // Given
        whenever(bankService.getCardInfo("1234567890123456")).thenReturn(testCard)
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }

        // When
        val result = atmService.insertCard("1234567890123456")

        // Then
        assertNotNull(result.sessionId)
        assertTrue(result.requiresPin)
        verify(sessionRepository).save(any<ATMSession>())
    }

    @Test
    fun `insertCard should throw exception for invalid card`() {
        // Given
        whenever(bankService.getCardInfo("invalid")).thenReturn(null)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            atmService.insertCard("invalid")
        }
        assertEquals("Invalid card", exception.message)
    }

    @Test
    fun `insertCard should throw exception for blocked card`() {
        // Given
        val blockedCard = Card(
            cardNumber = "1234567890123456",
            pin = "1234",
            cardHolderName = "Test User",
            isActive = false
        )
        whenever(bankService.getCardInfo("1234567890123456")).thenReturn(blockedCard)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.insertCard("1234567890123456")
        }
        assertEquals("Card is blocked", exception.message)
    }

    @Test
    fun `verifyPin should return success for correct PIN`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.CARD_INSERTED
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.validatePin(testCard.cardNumber, "1234")).thenReturn(true)
        whenever(bankService.getAccounts(testCard.cardNumber)).thenReturn(listOf(testAccount))
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }

        // When
        val result = atmService.verifyPin(sessionId, "1234")

        // Then
        assertTrue(result.verified)
        assertEquals(1, result.accounts.size)
        assertNull(result.remainingAttempts)
        assertFalse(result.cardBlocked)
        verify(sessionRepository).save(any<ATMSession>())
    }

    @Test
    fun `verifyPin should track failed attempts for wrong PIN`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.CARD_INSERTED,
            pinAttempts = 0
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.validatePin(testCard.cardNumber, "9999")).thenReturn(false)
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }

        // When
        val result = atmService.verifyPin(sessionId, "9999")

        // Then
        assertFalse(result.verified)
        assertEquals(2, result.remainingAttempts) // 3 - 1 = 2
        assertFalse(result.cardBlocked)
        assertEquals(1, session.pinAttempts)
    }

    @Test
    fun `verifyPin should block card after 3 failed attempts`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.CARD_INSERTED,
            pinAttempts = 2 // Already 2 failed attempts
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)
        whenever(bankService.validatePin(testCard.cardNumber, "9999")).thenReturn(false)
        whenever(sessionRepository.save(any<ATMSession>())).thenAnswer { it.arguments[0] as ATMSession }
        whenever(bankService.blockCard(testCard.cardNumber)).thenReturn(true)

        // When
        val result = atmService.verifyPin(sessionId, "9999")

        // Then
        assertFalse(result.verified)
        assertEquals(0, result.remainingAttempts)
        assertTrue(result.cardBlocked)
        assertEquals(SessionStatus.CARD_BLOCKED, session.sessionStatus)
        verify(bankService).blockCard(testCard.cardNumber)
    }

    @Test
    fun `verifyPin should throw exception for invalid session`() {
        // Given
        whenever(sessionRepository.findBySessionIdAndSessionStatusNot("invalid", SessionStatus.CLOSED))
            .thenReturn(null)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            atmService.verifyPin("invalid", "1234")
        }
        assertEquals("Invalid session", exception.message)
    }

    @Test
    fun `verifyPin should throw exception for wrong session state`() {
        // Given
        val sessionId = "test-session-id"
        val session = ATMSession(
            sessionId = sessionId,
            card = testCard,
            sessionStatus = SessionStatus.ACCOUNT_SELECTED // Wrong state
        )

        whenever(sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED))
            .thenReturn(session)

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            atmService.verifyPin(sessionId, "1234")
        }
        assertEquals("Invalid session state", exception.message)
    }
}