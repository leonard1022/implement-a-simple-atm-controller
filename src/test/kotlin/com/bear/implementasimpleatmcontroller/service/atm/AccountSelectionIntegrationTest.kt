package com.bear.implementasimpleatmcontroller.service.atm

import com.bear.implementasimpleatmcontroller.domain.*
import com.bear.implementasimpleatmcontroller.repository.ATMSessionRepository
import com.bear.implementasimpleatmcontroller.repository.AccountRepository
import com.bear.implementasimpleatmcontroller.repository.CardRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class AccountSelectionIntegrationTest {

    @Autowired
    private lateinit var atmService: ATMService

    @Autowired
    private lateinit var cardRepository: CardRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var sessionRepository: ATMSessionRepository

    private lateinit var testCard: Card
    private lateinit var checkingAccount: Account
    private lateinit var savingsAccount: Account
    private lateinit var businessAccount: Account

    @BeforeEach
    fun setUp() {
        // Create test card
        testCard = cardRepository.save(
            Card(
                cardNumber = "4444333322221111",
                pin = "4321",
                cardHolderName = "Account Selection Test User",
                isActive = true
            )
        )

        // Create multiple accounts with different types and balances
        checkingAccount = accountRepository.save(
            Account(
                accountNumber = "CHK-TEST-001",
                accountType = AccountType.CHECKING,
                balance = 1500,
                card = testCard
            )
        )

        savingsAccount = accountRepository.save(
            Account(
                accountNumber = "SAV-TEST-001",
                accountType = AccountType.SAVINGS,
                balance = 10000,
                card = testCard
            )
        )

        // Create another checking account to test multiple accounts of same type
        businessAccount = accountRepository.save(
            Account(
                accountNumber = "CHK-TEST-002",
                accountType = AccountType.CHECKING,
                balance = 50000,
                card = testCard
            )
        )
    }

    @Test
    fun `complete flow from card insertion to account selection`() {
        // Step 1: Insert card
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId

        // Step 2: Verify PIN
        val pinResult = atmService.verifyPin(sessionId, testCard.pin)
        assertTrue(pinResult.verified)
        assertEquals(3, pinResult.accounts.size) // Should have 3 accounts

        // Verify all accounts are returned
        val accountNumbers = pinResult.accounts.map { it.accountNumber }
        assertTrue(accountNumbers.contains(checkingAccount.accountNumber))
        assertTrue(accountNumbers.contains(savingsAccount.accountNumber))
        assertTrue(accountNumbers.contains(businessAccount.accountNumber))

        // Step 3: Select checking account
        val selectionResult = atmService.selectAccount(sessionId, checkingAccount.accountNumber)
        assertEquals(checkingAccount.accountNumber, selectionResult.account.accountNumber)
        assertEquals(AccountType.CHECKING, selectionResult.account.accountType)
        assertEquals(1500, selectionResult.account.balance)

        // Verify session state
        val session = sessionRepository.findBySessionId(sessionId)
        assertNotNull(session)
        assertEquals(SessionStatus.ACCOUNT_SELECTED, session?.sessionStatus)
        assertEquals(checkingAccount.id, session?.selectedAccount?.id)
    }

    @Test
    fun `can switch between different accounts in same session`() {
        // Setup: Get to PIN verified state
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)

        // Select checking account first
        var result = atmService.selectAccount(sessionId, checkingAccount.accountNumber)
        assertEquals(checkingAccount.accountNumber, result.account.accountNumber)

        // After first selection, we cannot switch directly (need new session or specific flow)
        // This tests the business rule that once selected, cannot reselect
        assertThrows<IllegalStateException> {
            atmService.selectAccount(sessionId, savingsAccount.accountNumber)
        }
    }

    @Test
    fun `selecting non-existent account should fail`() {
        // Setup: Get to PIN verified state
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)

        // Try to select account that doesn't exist
        assertThrows<IllegalArgumentException> {
            atmService.selectAccount(sessionId, "NON-EXISTENT-001")
        }

        // Session should still be in PIN_VERIFIED state
        val session = sessionRepository.findBySessionId(sessionId)
        assertEquals(SessionStatus.PIN_VERIFIED, session?.sessionStatus)
        assertNull(session?.selectedAccount)
    }

    @Test
    fun `cannot select account without PIN verification`() {
        // Insert card but don't verify PIN
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId

        // Try to select account without PIN verification
        assertThrows<IllegalStateException> {
            atmService.selectAccount(sessionId, checkingAccount.accountNumber)
        }
    }

    @Test
    fun `selecting account from different card should fail`() {
        // Create another card with its own account
        val anotherCard = cardRepository.save(
            Card(
                cardNumber = "9999888877776666",
                pin = "9999",
                cardHolderName = "Another User",
                isActive = true
            )
        )

        val anotherAccount = accountRepository.save(
            Account(
                accountNumber = "OTHER-001",
                accountType = AccountType.CHECKING,
                balance = 100,
                card = anotherCard
            )
        )

        // Use first card but try to select second card's account
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)

        // Should not be able to select account from different card
        assertThrows<IllegalArgumentException> {
            atmService.selectAccount(sessionId, anotherAccount.accountNumber)
        }
    }

    @Test
    fun `account selection persists across service calls`() {
        // Complete flow to account selection
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.selectAccount(sessionId, savingsAccount.accountNumber)

        // Verify the selection persists in database
        val session = sessionRepository.findBySessionId(sessionId)
        assertNotNull(session)
        assertEquals(savingsAccount.id, session?.selectedAccount?.id)
        assertEquals(savingsAccount.accountNumber, session?.selectedAccount?.accountNumber)
        assertEquals(savingsAccount.accountType, session?.selectedAccount?.accountType)
        assertEquals(savingsAccount.balance, session?.selectedAccount?.balance)
    }

    @Test
    fun `verify account types are correctly identified`() {
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        val pinResult = atmService.verifyPin(sessionId, testCard.pin)

        // Check that account types are correctly returned
        val checkingAccounts = pinResult.accounts.filter { it.accountType == AccountType.CHECKING }
        val savingsAccounts = pinResult.accounts.filter { it.accountType == AccountType.SAVINGS }

        assertEquals(2, checkingAccounts.size) // We have 2 checking accounts
        assertEquals(1, savingsAccounts.size)  // We have 1 savings account

        // Select savings account and verify type
        val result = atmService.selectAccount(sessionId, savingsAccount.accountNumber)
        assertEquals(AccountType.SAVINGS, result.account.accountType)
    }

    @Test
    fun `closed session cannot select account`() {
        // Complete flow and close session
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.endSession(sessionId)

        // Try to select account on closed session
        assertThrows<IllegalArgumentException> {
            atmService.selectAccount(sessionId, checkingAccount.accountNumber)
        }
    }
}