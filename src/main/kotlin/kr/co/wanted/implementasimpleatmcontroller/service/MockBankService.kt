package kr.co.wanted.implementasimpleatmcontroller.service

import kr.co.wanted.implementasimpleatmcontroller.domain.Account
import kr.co.wanted.implementasimpleatmcontroller.domain.Card
import kr.co.wanted.implementasimpleatmcontroller.repository.AccountRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MockBankService(
    private val cardRepository: CardRepository,
    private val accountRepository: AccountRepository
) : BankService {

    override fun validatePin(cardNumber: String, pin: String): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber)
        return card != null && card.pin == pin && card.isActive
    }

    override fun getCardInfo(cardNumber: String): Card? {
        return cardRepository.findByCardNumber(cardNumber)
    }

    override fun getAccounts(cardNumber: String): List<Account> {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return emptyList()
        return accountRepository.findByCard(card)
    }

    override fun getBalance(accountNumber: String): Int {
        val account = accountRepository.findByAccountNumber(accountNumber)
        return account?.balance ?: 0
    }

    override fun deposit(accountNumber: String, amount: Int): Boolean {
        val account = accountRepository.findByAccountNumber(accountNumber) ?: return false
        return try {
            account.deposit(amount)
            accountRepository.save(account)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun withdraw(accountNumber: String, amount: Int): Boolean {
        val account = accountRepository.findByAccountNumber(accountNumber) ?: return false
        return try {
            account.withdraw(amount)
            accountRepository.save(account)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun blockCard(cardNumber: String): Boolean {
        val card = cardRepository.findByCardNumber(cardNumber) ?: return false
        card.isActive = false
        cardRepository.save(card)
        return true
    }
}