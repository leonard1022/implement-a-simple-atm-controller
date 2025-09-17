package com.bear.implementasimpleatmcontroller.service.atm

import com.bear.implementasimpleatmcontroller.domain.Account
import com.bear.implementasimpleatmcontroller.domain.AccountType
import com.bear.implementasimpleatmcontroller.domain.Card
import com.bear.implementasimpleatmcontroller.domain.SessionStatus
import com.bear.implementasimpleatmcontroller.repository.ATMSessionRepository
import com.bear.implementasimpleatmcontroller.repository.AccountRepository
import com.bear.implementasimpleatmcontroller.repository.CardRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class CardAndPinIntegrationTest {

    @Autowired
    private lateinit var atmService: ATMService

    @Autowired
    private lateinit var cardRepository: CardRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var sessionRepository: ATMSessionRepository

    private lateinit var testCard: Card
    private lateinit var testAccount1: Account
    private lateinit var testAccount2: Account

    @BeforeEach
    fun setUp() {
        // Create test card
        testCard = cardRepository.save(
            Card(
                cardNumber = "1111222233334444",
                pin = "1111",
                cardHolderName = "Integration Test User",
                isActive = true
            )
        )

        // Create test accounts
        testAccount1 = accountRepository.save(
            Account(
                accountNumber = "INT001",
                accountType = AccountType.CHECKING,
                balance = 5000,
                card = testCard
            )
        )

        testAccount2 = accountRepository.save(
            Account(
                accountNumber = "INT002",
                accountType = AccountType.SAVINGS,
                balance = 10000,
                card = testCard
            )
        )
    }

    @Test
    fun `complete card insertion and PIN verification flow`() {
        // Step 1: Insert Card
        val insertResult = atmService.insertCard(testCard.cardNumber)
        assertNotNull(insertResult.sessionId)
        assertTrue(insertResult.requiresPin)

        val sessionId = insertResult.sessionId

        // Verify session was created
        val session = sessionRepository.findBySessionId(sessionId)
        assertNotNull(session)
        assertEquals(SessionStatus.CARD_INSERTED, session?.sessionStatus)
        assertEquals(testCard.id, session?.card?.id)

        // Step 2: Verify PIN with wrong PIN first
        val wrongPinResult = atmService.verifyPin(sessionId, "9999")
        assertFalse(wrongPinResult.verified)
        assertEquals(2, wrongPinResult.remainingAttempts)
        assertFalse(wrongPinResult.cardBlocked)

        // Step 3: Verify PIN with correct PIN
        val correctPinResult = atmService.verifyPin(sessionId, testCard.pin)
        assertTrue(correctPinResult.verified)
        assertEquals(2, correctPinResult.accounts.size)
        assertFalse(correctPinResult.cardBlocked)

        // Verify accounts returned
        val accountNumbers = correctPinResult.accounts.map { it.accountNumber }
        assertTrue(accountNumbers.contains(testAccount1.accountNumber))
        assertTrue(accountNumbers.contains(testAccount2.accountNumber))

        // Verify session status updated
        val updatedSession = sessionRepository.findBySessionId(sessionId)
        assertEquals(SessionStatus.PIN_VERIFIED, updatedSession?.sessionStatus)
    }

    @Test
    fun `card blocking after maximum PIN attempts`() {
        // Insert card
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId

        // Attempt 1 - Wrong PIN
        var result = atmService.verifyPin(sessionId, "0000")
        assertFalse(result.verified)
        assertEquals(2, result.remainingAttempts)

        // Attempt 2 - Wrong PIN
        result = atmService.verifyPin(sessionId, "9999")
        assertFalse(result.verified)
        assertEquals(1, result.remainingAttempts)

        // Attempt 3 - Wrong PIN - Should block card
        result = atmService.verifyPin(sessionId, "2222")
        assertFalse(result.verified)
        assertEquals(0, result.remainingAttempts)
        assertTrue(result.cardBlocked)

        // Verify session status
        val session = sessionRepository.findBySessionId(sessionId)
        assertEquals(SessionStatus.CARD_BLOCKED, session?.sessionStatus)

        // Verify card is blocked in database
        val blockedCard = cardRepository.findByCardNumber(testCard.cardNumber)
        assertFalse(blockedCard?.isActive ?: true)
    }

    @Test
    fun `cannot use blocked card`() {
        // Block the card first
        testCard.isActive = false
        cardRepository.save(testCard)

        // Try to insert blocked card
        assertThrows(IllegalStateException::class.java) {
            atmService.insertCard(testCard.cardNumber)
        }
    }

    @Test
    fun `session isolation between different cards`() {
        // Create another card
        val anotherCard = cardRepository.save(
            Card(
                cardNumber = "5555666677778888",
                pin = "5678",
                cardHolderName = "Another User",
                isActive = true
            )
        )

        // Insert first card
        val session1 = atmService.insertCard(testCard.cardNumber)

        // Insert second card
        val session2 = atmService.insertCard(anotherCard.cardNumber)

        // Sessions should be different
        assertNotEquals(session1.sessionId, session2.sessionId)

        // Verify PIN for first card shouldn't affect second card
        atmService.verifyPin(session1.sessionId, "9999") // Wrong PIN for first card

        // Second card should still work normally
        val result = atmService.verifyPin(session2.sessionId, anotherCard.pin)
        assertTrue(result.verified)
    }

    @Test
    fun `cannot verify PIN without inserting card first`() {
        assertThrows(IllegalArgumentException::class.java) {
            atmService.verifyPin("non-existent-session", "1234")
        }
    }

    @Test
    fun `cannot verify PIN twice after successful verification`() {
        // Insert card and verify PIN successfully
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId

        val firstVerification = atmService.verifyPin(sessionId, testCard.pin)
        assertTrue(firstVerification.verified)

        // Try to verify PIN again
        assertThrows(IllegalStateException::class.java) {
            atmService.verifyPin(sessionId, testCard.pin)
        }
    }
}