package kr.co.wanted.implementasimpleatmcontroller.service.atm

import kr.co.wanted.implementasimpleatmcontroller.domain.*
import kr.co.wanted.implementasimpleatmcontroller.repository.ATMSessionRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.TransactionRepository
import kr.co.wanted.implementasimpleatmcontroller.service.atm.model.*
import kr.co.wanted.implementasimpleatmcontroller.service.bank.BankService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ATMServiceImpl(
    private val bankService: BankService,
    private val sessionRepository: ATMSessionRepository,
    private val transactionRepository: TransactionRepository
) : ATMService {

    override fun insertCard(cardNumber: String): CardInsertionResult {
        val card = bankService.getCardInfo(cardNumber)
            ?: throw IllegalArgumentException("Invalid card")

        if (!card.isActive) {
            throw IllegalStateException("Card is blocked")
        }

        val sessionId = UUID.randomUUID().toString()
        val session = ATMSession(
            sessionId = sessionId,
            card = card,
            sessionStatus = SessionStatus.CARD_INSERTED
        )
        sessionRepository.save(session)

        return CardInsertionResult(sessionId = sessionId)
    }

    override fun verifyPin(sessionId: String, pin: String): PinVerificationResult {
        val session = getActiveSession(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        if (session.sessionStatus != SessionStatus.CARD_INSERTED) {
            throw IllegalStateException("Invalid session state")
        }

        val card = session.card
            ?: throw IllegalStateException("No card in session")

        if (!bankService.validatePin(card.cardNumber, pin)) {
            session.incrementPinAttempts()
            sessionRepository.save(session)

            if (session.sessionStatus == SessionStatus.CARD_BLOCKED) {
                bankService.blockCard(card.cardNumber)
                return PinVerificationResult(
                    verified = false,
                    remainingAttempts = 0,
                    cardBlocked = true
                )
            }

            val remaining = ATMSession.MAX_PIN_ATTEMPTS - session.pinAttempts
            return PinVerificationResult(
                verified = false,
                remainingAttempts = remaining
            )
        }

        session.sessionStatus = SessionStatus.PIN_VERIFIED
        sessionRepository.save(session)

        val accounts = bankService.getAccounts(card.cardNumber)

        return PinVerificationResult(
            verified = true,
            accounts = accounts
        )
    }

    override fun selectAccount(sessionId: String, accountNumber: String): AccountSelectionResult {
        val session = getActiveSession(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        if (session.sessionStatus != SessionStatus.PIN_VERIFIED) {
            throw IllegalStateException("PIN not verified")
        }

        val card = session.card
            ?: throw IllegalStateException("No card in session")

        val accounts = bankService.getAccounts(card.cardNumber)
        val selectedAccount = accounts.find { it.accountNumber == accountNumber }
            ?: throw IllegalArgumentException("Invalid account number")

        session.selectedAccount = selectedAccount
        session.sessionStatus = SessionStatus.ACCOUNT_SELECTED
        sessionRepository.save(session)

        return AccountSelectionResult(account = selectedAccount)
    }

    override fun checkBalance(sessionId: String): BalanceResult {
        val session = getActiveSession(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        if (session.sessionStatus != SessionStatus.ACCOUNT_SELECTED) {
            throw IllegalStateException("No account selected")
        }

        val account = session.selectedAccount
            ?: throw IllegalStateException("No account in session")

        val balance = bankService.getBalance(account.accountNumber)

        val transaction = Transaction(
            transactionType = TransactionType.BALANCE_INQUIRY,
            amount = 0,
            balanceAfter = balance,
            account = account
        )
        transactionRepository.save(transaction)

        return BalanceResult(
            accountNumber = account.accountNumber,
            balance = balance
        )
    }

    override fun deposit(sessionId: String, amount: Int): DepositResult {
        require(amount > 0) { "Amount must be positive" }

        val session = getActiveSession(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        if (session.sessionStatus != SessionStatus.ACCOUNT_SELECTED) {
            throw IllegalStateException("No account selected")
        }

        val account = session.selectedAccount
            ?: throw IllegalStateException("No account in session")

        val previousBalance = bankService.getBalance(account.accountNumber)

        if (!bankService.deposit(account.accountNumber, amount)) {
            throw IllegalStateException("Deposit failed")
        }

        val newBalance = bankService.getBalance(account.accountNumber)

        val transaction = Transaction(
            transactionType = TransactionType.DEPOSIT,
            amount = amount,
            balanceAfter = newBalance,
            account = account
        )
        transactionRepository.save(transaction)

        return DepositResult(
            previousBalance = previousBalance,
            depositedAmount = amount,
            newBalance = newBalance
        )
    }

    override fun withdraw(sessionId: String, amount: Int): WithdrawalResult {
        require(amount > 0) { "Amount must be positive" }

        val session = getActiveSession(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        if (session.sessionStatus != SessionStatus.ACCOUNT_SELECTED) {
            throw IllegalStateException("No account selected")
        }

        val account = session.selectedAccount
            ?: throw IllegalStateException("No account in session")

        val previousBalance = bankService.getBalance(account.accountNumber)

        if (previousBalance < amount) {
            throw IllegalArgumentException("Insufficient balance")
        }

        if (!bankService.withdraw(account.accountNumber, amount)) {
            throw IllegalStateException("Withdrawal failed")
        }

        val newBalance = bankService.getBalance(account.accountNumber)

        val transaction = Transaction(
            transactionType = TransactionType.WITHDRAWAL,
            amount = amount,
            balanceAfter = newBalance,
            account = account
        )
        transactionRepository.save(transaction)

        return WithdrawalResult(
            previousBalance = previousBalance,
            withdrawnAmount = amount,
            newBalance = newBalance
        )
    }

    override fun endSession(sessionId: String): Boolean {
        val session = sessionRepository.findBySessionId(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        if (session.sessionStatus == SessionStatus.CLOSED) {
            return false
        }

        session.closeSession()
        sessionRepository.save(session)

        return true
    }

    private fun getActiveSession(sessionId: String): ATMSession? {
        return sessionRepository.findBySessionIdAndSessionStatusNot(sessionId, SessionStatus.CLOSED)
    }
}