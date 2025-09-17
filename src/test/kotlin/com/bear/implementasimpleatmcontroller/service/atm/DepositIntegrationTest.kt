package com.bear.implementasimpleatmcontroller.service.atm

import com.bear.implementasimpleatmcontroller.domain.Account
import com.bear.implementasimpleatmcontroller.domain.AccountType
import com.bear.implementasimpleatmcontroller.domain.Card
import com.bear.implementasimpleatmcontroller.domain.SessionStatus
import com.bear.implementasimpleatmcontroller.repository.ATMSessionRepository
import com.bear.implementasimpleatmcontroller.repository.AccountRepository
import com.bear.implementasimpleatmcontroller.repository.CardRepository
import com.bear.implementasimpleatmcontroller.repository.TransactionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class DepositIntegrationTest {

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
    private lateinit var testAccount: Account

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        sessionRepository.deleteAll()
        accountRepository.deleteAll()
        cardRepository.deleteAll()

        testCard = cardRepository.save(
            Card(
                cardNumber = "1111222233334444",
                pin = "1234",
                cardHolderName = "Test User",
                isActive = true
            )
        )

        testAccount = accountRepository.save(
            Account(
                accountNumber = "TEST001",
                accountType = AccountType.CHECKING,
                balance = 1000,
                card = testCard
            )
        )
    }

    @Test
    fun `successfully deposit money to checking account`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        val pinResult = atmService.verifyPin(sessionId, "1234")
        assertThat(pinResult.verified).isTrue()

        atmService.selectAccount(sessionId, "TEST001")

        val depositResult = atmService.deposit(sessionId, 500)

        assertThat(depositResult.previousBalance).isEqualTo(1000)
        assertThat(depositResult.depositedAmount).isEqualTo(500)
        assertThat(depositResult.newBalance).isEqualTo(1500)

        val updatedAccount = accountRepository.findById(testAccount.id!!).orElseThrow()
        assertThat(updatedAccount.balance).isEqualTo(1500)

        val transactions = transactionRepository.findAll()
        assertThat(transactions).hasSize(1)
        val transaction = transactions.first()
        assertThat(transaction.transactionType).isEqualTo(com.bear.implementasimpleatmcontroller.domain.TransactionType.DEPOSIT)
        assertThat(transaction.amount).isEqualTo(500)
        assertThat(transaction.balanceAfter).isEqualTo(1500)
    }

    @Test
    fun `deposit multiple times in same session`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "TEST001")

        val firstDeposit = atmService.deposit(sessionId, 200)
        assertThat(firstDeposit.newBalance).isEqualTo(1200)

        val secondDeposit = atmService.deposit(sessionId, 300)
        assertThat(secondDeposit.previousBalance).isEqualTo(1200)
        assertThat(secondDeposit.newBalance).isEqualTo(1500)

        val transactions = transactionRepository.findAll()
        assertThat(transactions).hasSize(2)
    }

    @Test
    fun `fail to deposit with negative amount`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "TEST001")

        val exception = assertThrows<IllegalArgumentException> {
            atmService.deposit(sessionId, -100)
        }
        assertThat(exception.message).isEqualTo("Amount must be positive")
    }

    @Test
    fun `fail to deposit with zero amount`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "TEST001")

        val exception = assertThrows<IllegalArgumentException> {
            atmService.deposit(sessionId, 0)
        }
        assertThat(exception.message).isEqualTo("Amount must be positive")
    }

    @Test
    fun `fail to deposit without selecting account`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")

        val exception = assertThrows<IllegalStateException> {
            atmService.deposit(sessionId, 500)
        }
        assertThat(exception.message).isEqualTo("No account selected")
    }

    @Test
    fun `fail to deposit without PIN verification`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        val exception = assertThrows<IllegalStateException> {
            atmService.deposit(sessionId, 500)
        }
        assertThat(exception.message).isEqualTo("No account selected")

        val session = sessionRepository.findBySessionId(sessionId)
        assertThat(session).isNotNull
        assertThat(session!!.sessionStatus).isEqualTo(SessionStatus.CARD_INSERTED)
    }

    @Test
    fun `fail to deposit with invalid session`() {
        val exception = assertThrows<IllegalArgumentException> {
            atmService.deposit("invalid-session", 500)
        }
        assertThat(exception.message).isEqualTo("Invalid session")
    }

    @Test
    fun `deposit to savings account`() {
        val savingsAccount = accountRepository.save(
            Account(
                accountNumber = "TEST002",
                accountType = AccountType.SAVINGS,
                balance = 5000,
                card = testCard
            )
        )

        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "TEST002")

        val depositResult = atmService.deposit(sessionId, 1000)

        assertThat(depositResult.previousBalance).isEqualTo(5000)
        assertThat(depositResult.depositedAmount).isEqualTo(1000)
        assertThat(depositResult.newBalance).isEqualTo(6000)

        val updatedAccount = accountRepository.findById(savingsAccount.id!!).orElseThrow()
        assertThat(updatedAccount.balance).isEqualTo(6000)
    }

    @Test
    fun `deposit large amount`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "TEST001")

        val depositResult = atmService.deposit(sessionId, 100000)

        assertThat(depositResult.previousBalance).isEqualTo(1000)
        assertThat(depositResult.depositedAmount).isEqualTo(100000)
        assertThat(depositResult.newBalance).isEqualTo(101000)
    }

    @Test
    fun `verify session remains active after deposit`() {
        val insertResult = atmService.insertCard("1111222233334444")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "TEST001")
        atmService.deposit(sessionId, 500)

        val session = sessionRepository.findBySessionId(sessionId)
        assertThat(session).isNotNull
        assertThat(session!!.sessionStatus).isEqualTo(SessionStatus.ACCOUNT_SELECTED)
        assertThat(session.closedAt).isNull()

        val balanceResult = atmService.checkBalance(sessionId)
        assertThat(balanceResult.balance).isEqualTo(1500)
    }
}