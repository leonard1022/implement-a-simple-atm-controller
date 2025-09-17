package com.bear.implementasimpleatmcontroller.service.atm

import com.bear.implementasimpleatmcontroller.domain.*
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
class WithdrawalIntegrationTest {

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
                cardNumber = "5555666677778888",
                pin = "1234",
                cardHolderName = "Test User",
                isActive = true
            )
        )

        testAccount = accountRepository.save(
            Account(
                accountNumber = "WITHDRAW001",
                accountType = AccountType.CHECKING,
                balance = 1000,
                card = testCard
            )
        )
    }

    @Test
    fun `successfully withdraw money from checking account`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        val pinResult = atmService.verifyPin(sessionId, "1234")
        assertThat(pinResult.verified).isTrue()

        atmService.selectAccount(sessionId, "WITHDRAW001")

        val withdrawResult = atmService.withdraw(sessionId, 300)

        assertThat(withdrawResult.previousBalance).isEqualTo(1000)
        assertThat(withdrawResult.withdrawnAmount).isEqualTo(300)
        assertThat(withdrawResult.newBalance).isEqualTo(700)

        val updatedAccount = accountRepository.findById(testAccount.id!!).orElseThrow()
        assertThat(updatedAccount.balance).isEqualTo(700)

        val transactions = transactionRepository.findAll()
        assertThat(transactions).hasSize(1)
        val transaction = transactions.first()
        assertThat(transaction.transactionType).isEqualTo(TransactionType.WITHDRAWAL)
        assertThat(transaction.amount).isEqualTo(300)
        assertThat(transaction.balanceAfter).isEqualTo(700)
    }

    @Test
    fun `withdraw multiple times in same session`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")

        val firstWithdraw = atmService.withdraw(sessionId, 200)
        assertThat(firstWithdraw.newBalance).isEqualTo(800)

        val secondWithdraw = atmService.withdraw(sessionId, 300)
        assertThat(secondWithdraw.previousBalance).isEqualTo(800)
        assertThat(secondWithdraw.newBalance).isEqualTo(500)

        val transactions = transactionRepository.findAll()
        assertThat(transactions).hasSize(2)
    }

    @Test
    fun `fail to withdraw with insufficient balance`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")

        val exception = assertThrows<IllegalArgumentException> {
            atmService.withdraw(sessionId, 1500)
        }
        assertThat(exception.message).isEqualTo("Insufficient balance")

        val account = accountRepository.findById(testAccount.id!!).orElseThrow()
        assertThat(account.balance).isEqualTo(1000)
    }

    @Test
    fun `fail to withdraw with negative amount`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")

        val exception = assertThrows<IllegalArgumentException> {
            atmService.withdraw(sessionId, -100)
        }
        assertThat(exception.message).isEqualTo("Amount must be positive")
    }

    @Test
    fun `fail to withdraw with zero amount`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")

        val exception = assertThrows<IllegalArgumentException> {
            atmService.withdraw(sessionId, 0)
        }
        assertThat(exception.message).isEqualTo("Amount must be positive")
    }

    @Test
    fun `fail to withdraw without selecting account`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")

        val exception = assertThrows<IllegalStateException> {
            atmService.withdraw(sessionId, 500)
        }
        assertThat(exception.message).isEqualTo("No account selected")
    }

    @Test
    fun `fail to withdraw without PIN verification`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        val exception = assertThrows<IllegalStateException> {
            atmService.withdraw(sessionId, 500)
        }
        assertThat(exception.message).isEqualTo("No account selected")

        val session = sessionRepository.findBySessionId(sessionId)
        assertThat(session).isNotNull
        assertThat(session!!.sessionStatus).isEqualTo(SessionStatus.CARD_INSERTED)
    }

    @Test
    fun `fail to withdraw with invalid session`() {
        val exception = assertThrows<IllegalArgumentException> {
            atmService.withdraw("invalid-session", 500)
        }
        assertThat(exception.message).isEqualTo("Invalid session")
    }

    @Test
    fun `withdraw from savings account`() {
        val savingsAccount = accountRepository.save(
            Account(
                accountNumber = "WITHDRAW002",
                accountType = AccountType.SAVINGS,
                balance = 5000,
                card = testCard
            )
        )

        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW002")

        val withdrawResult = atmService.withdraw(sessionId, 1000)

        assertThat(withdrawResult.previousBalance).isEqualTo(5000)
        assertThat(withdrawResult.withdrawnAmount).isEqualTo(1000)
        assertThat(withdrawResult.newBalance).isEqualTo(4000)

        val updatedAccount = accountRepository.findById(savingsAccount.id!!).orElseThrow()
        assertThat(updatedAccount.balance).isEqualTo(4000)
    }

    @Test
    fun `withdraw entire balance`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")

        val withdrawResult = atmService.withdraw(sessionId, 1000)

        assertThat(withdrawResult.previousBalance).isEqualTo(1000)
        assertThat(withdrawResult.withdrawnAmount).isEqualTo(1000)
        assertThat(withdrawResult.newBalance).isEqualTo(0)

        val updatedAccount = accountRepository.findById(testAccount.id!!).orElseThrow()
        assertThat(updatedAccount.balance).isEqualTo(0)
    }

    @Test
    fun `fail to withdraw more than balance by one`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")

        val exception = assertThrows<IllegalArgumentException> {
            atmService.withdraw(sessionId, 1001)
        }
        assertThat(exception.message).isEqualTo("Insufficient balance")
    }

    @Test
    fun `verify session remains active after withdrawal`() {
        val insertResult = atmService.insertCard("5555666677778888")
        val sessionId = insertResult.sessionId

        atmService.verifyPin(sessionId, "1234")
        atmService.selectAccount(sessionId, "WITHDRAW001")
        atmService.withdraw(sessionId, 500)

        val session = sessionRepository.findBySessionId(sessionId)
        assertThat(session).isNotNull
        assertThat(session!!.sessionStatus).isEqualTo(SessionStatus.ACCOUNT_SELECTED)
        assertThat(session.closedAt).isNull()

        val balanceResult = atmService.checkBalance(sessionId)
        assertThat(balanceResult.balance).isEqualTo(500)
    }
}
