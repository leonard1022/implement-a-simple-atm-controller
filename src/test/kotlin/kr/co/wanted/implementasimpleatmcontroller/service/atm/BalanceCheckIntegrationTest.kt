package kr.co.wanted.implementasimpleatmcontroller.service.atm

import kr.co.wanted.implementasimpleatmcontroller.domain.*
import kr.co.wanted.implementasimpleatmcontroller.repository.ATMSessionRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.AccountRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.CardRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class BalanceCheckIntegrationTest {

    @Autowired
    private lateinit var atmService: ATMService

    @Autowired
    private lateinit var cardRepository: CardRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var sessionRepository: ATMSessionRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var testCard: Card
    private lateinit var checkingAccount: Account
    private lateinit var savingsAccount: Account

    @BeforeEach
    fun setUp() {
        // Create test card
        testCard = cardRepository.save(
            Card(
                cardNumber = "5555444433332222",
                pin = "5555",
                cardHolderName = "Balance Check Test User",
                isActive = true
            )
        )

        // Create test accounts with known balances
        checkingAccount = accountRepository.save(
            Account(
                accountNumber = "BAL-CHK-001",
                accountType = AccountType.CHECKING,
                balance = 2500,
                card = testCard
            )
        )

        savingsAccount = accountRepository.save(
            Account(
                accountNumber = "BAL-SAV-001",
                accountType = AccountType.SAVINGS,
                balance = 15000,
                card = testCard
            )
        )
    }

    @Test
    fun `complete flow from card insertion to balance check`() {
        // Step 1: Insert card
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId

        // Step 2: Verify PIN
        val pinResult = atmService.verifyPin(sessionId, testCard.pin)
        assertTrue(pinResult.verified)

        // Step 3: Select checking account
        atmService.selectAccount(sessionId, checkingAccount.accountNumber)

        // Step 4: Check balance
        val balanceResult = atmService.checkBalance(sessionId)
        assertEquals(checkingAccount.accountNumber, balanceResult.accountNumber)
        assertEquals(2500, balanceResult.balance)

        // Verify transaction was recorded
        val transactions = transactionRepository.findAll()
        val balanceInquiry = transactions.find {
            it.transactionType == TransactionType.BALANCE_INQUIRY &&
            it.account?.accountNumber == checkingAccount.accountNumber
        }
        assertNotNull(balanceInquiry)
        assertEquals(0, balanceInquiry?.amount)
        assertEquals(2500, balanceInquiry?.balanceAfter)
    }

    @Test
    fun `check balance for different account types`() {
        // Setup session with savings account
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.selectAccount(sessionId, savingsAccount.accountNumber)

        // Check savings account balance
        val balanceResult = atmService.checkBalance(sessionId)
        assertEquals(savingsAccount.accountNumber, balanceResult.accountNumber)
        assertEquals(15000, balanceResult.balance)
    }

    @Test
    fun `multiple balance checks create separate transactions`() {
        // Setup session
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.selectAccount(sessionId, checkingAccount.accountNumber)

        // Check balance multiple times
        val result1 = atmService.checkBalance(sessionId)
        val result2 = atmService.checkBalance(sessionId)
        val result3 = atmService.checkBalance(sessionId)

        // All should return same balance
        assertEquals(2500, result1.balance)
        assertEquals(2500, result2.balance)
        assertEquals(2500, result3.balance)

        // Verify 3 separate transactions were created
        val transactions = transactionRepository.findAll()
        val balanceInquiries = transactions.filter {
            it.transactionType == TransactionType.BALANCE_INQUIRY
        }
        assertEquals(3, balanceInquiries.size)
    }

    @Test
    fun `balance check reflects account updates`() {
        // Setup session
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.selectAccount(sessionId, checkingAccount.accountNumber)

        // Check initial balance
        val initialBalance = atmService.checkBalance(sessionId)
        assertEquals(2500, initialBalance.balance)

        // Simulate external balance change (e.g., from another ATM or branch)
        val account = accountRepository.findByAccountNumber(checkingAccount.accountNumber)
        account?.balance = 3000
        accountRepository.save(account!!)

        // Check balance again - should reflect new balance
        val updatedBalance = atmService.checkBalance(sessionId)
        assertEquals(3000, updatedBalance.balance)
    }

    @Test
    fun `cannot check balance without selecting account`() {
        // Setup session but don't select account
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)

        // Try to check balance without account selection
        val exception = assertThrows<IllegalStateException> {
            atmService.checkBalance(sessionId)
        }
        assertEquals("No account selected", exception.message)
    }

    @Test
    fun `cannot check balance with invalid session`() {
        val exception = assertThrows<IllegalArgumentException> {
            atmService.checkBalance("invalid-session-id")
        }
        assertEquals("Invalid session", exception.message)
    }

    @Test
    fun `cannot check balance after session is closed`() {
        // Complete flow and close session
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.selectAccount(sessionId, checkingAccount.accountNumber)

        // Check balance works before closing
        val balanceResult = atmService.checkBalance(sessionId)
        assertEquals(2500, balanceResult.balance)

        // Close session
        atmService.endSession(sessionId)

        // Cannot check balance after session closed
        val exception = assertThrows<IllegalArgumentException> {
            atmService.checkBalance(sessionId)
        }
        assertEquals("Invalid session", exception.message)
    }

    @Test
    fun `balance check transaction includes session information`() {
        // Setup session
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId
        atmService.verifyPin(sessionId, testCard.pin)
        atmService.selectAccount(sessionId, checkingAccount.accountNumber)

        // Check balance
        atmService.checkBalance(sessionId)

        // Verify transaction has correct account info
        val transactions = transactionRepository.findAll()
        val balanceInquiry = transactions.find {
            it.transactionType == TransactionType.BALANCE_INQUIRY
        }

        assertNotNull(balanceInquiry)
        assertEquals(checkingAccount.id, balanceInquiry?.account?.id)
        assertEquals(0, balanceInquiry?.amount)
        assertEquals(2500, balanceInquiry?.balanceAfter)
    }

    @Test
    fun `balance check works after failed PIN attempt`() {
        // Insert card
        val insertResult = atmService.insertCard(testCard.cardNumber)
        val sessionId = insertResult.sessionId

        // First attempt with wrong PIN
        val wrongPinResult = atmService.verifyPin(sessionId, "9999")
        assertFalse(wrongPinResult.verified)

        // Second attempt with correct PIN
        val correctPinResult = atmService.verifyPin(sessionId, testCard.pin)
        assertTrue(correctPinResult.verified)

        // Select account and check balance
        atmService.selectAccount(sessionId, checkingAccount.accountNumber)
        val balanceResult = atmService.checkBalance(sessionId)

        assertEquals(2500, balanceResult.balance)

        // Verify only balance inquiry transaction exists (not PIN attempts)
        val transactions = transactionRepository.findAll()
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.BALANCE_INQUIRY, transactions[0].transactionType)
    }

    @Test
    fun `concurrent balance checks for different sessions`() {
        // Create another card with account
        val anotherCard = cardRepository.save(
            Card(
                cardNumber = "7777666655554444",
                pin = "7777",
                cardHolderName = "Another Balance User",
                isActive = true
            )
        )

        val anotherAccount = accountRepository.save(
            Account(
                accountNumber = "BAL-OTHER-001",
                accountType = AccountType.CHECKING,
                balance = 5000,
                card = anotherCard
            )
        )

        // Setup first session
        val session1Id = atmService.insertCard(testCard.cardNumber).sessionId
        atmService.verifyPin(session1Id, testCard.pin)
        atmService.selectAccount(session1Id, checkingAccount.accountNumber)

        // Setup second session
        val session2Id = atmService.insertCard(anotherCard.cardNumber).sessionId
        atmService.verifyPin(session2Id, anotherCard.pin)
        atmService.selectAccount(session2Id, anotherAccount.accountNumber)

        // Check balances for both sessions
        val balance1 = atmService.checkBalance(session1Id)
        val balance2 = atmService.checkBalance(session2Id)

        // Verify each session gets correct balance
        assertEquals(2500, balance1.balance)
        assertEquals(5000, balance2.balance)

        // Verify two separate transactions were created
        val transactions = transactionRepository.findAll()
        assertEquals(2, transactions.size)

        val session1Transaction = transactions.find {
            it.account?.accountNumber == checkingAccount.accountNumber
        }
        val session2Transaction = transactions.find {
            it.account?.accountNumber == anotherAccount.accountNumber
        }

        assertNotNull(session1Transaction)
        assertNotNull(session2Transaction)
        assertEquals(2500, session1Transaction?.balanceAfter)
        assertEquals(5000, session2Transaction?.balanceAfter)
    }
}