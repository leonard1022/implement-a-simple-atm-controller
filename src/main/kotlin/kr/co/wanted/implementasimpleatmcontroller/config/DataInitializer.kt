package kr.co.wanted.implementasimpleatmcontroller.config

import kr.co.wanted.implementasimpleatmcontroller.domain.Account
import kr.co.wanted.implementasimpleatmcontroller.domain.AccountType
import kr.co.wanted.implementasimpleatmcontroller.domain.Card
import kr.co.wanted.implementasimpleatmcontroller.repository.AccountRepository
import kr.co.wanted.implementasimpleatmcontroller.repository.CardRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataInitializer {

    @Bean
    fun init(
        cardRepository: CardRepository,
        accountRepository: AccountRepository
    ) = CommandLineRunner {
        val card1 = Card(
            cardNumber = "1234567890123456",
            pin = "1234",
            cardHolderName = "John Doe",
            isActive = true
        )
        val savedCard1 = cardRepository.save(card1)

        val card2 = Card(
            cardNumber = "9876543210987654",
            pin = "5678",
            cardHolderName = "Jane Smith",
            isActive = true
        )
        val savedCard2 = cardRepository.save(card2)

        accountRepository.save(
            Account(
                accountNumber = "ACC001",
                accountType = AccountType.CHECKING,
                balance = 1000,
                card = savedCard1
            )
        )

        accountRepository.save(
            Account(
                accountNumber = "ACC002",
                accountType = AccountType.SAVINGS,
                balance = 5000,
                card = savedCard1
            )
        )

        accountRepository.save(
            Account(
                accountNumber = "ACC003",
                accountType = AccountType.CHECKING,
                balance = 2000,
                card = savedCard2
            )
        )

        accountRepository.save(
            Account(
                accountNumber = "ACC004",
                accountType = AccountType.SAVINGS,
                balance = 10000,
                card = savedCard2
            )
        )

        println("Sample data initialized:")
        println("Card 1: 1234567890123456 (PIN: 1234)")
        println("  - Checking Account (ACC001): $1000")
        println("  - Savings Account (ACC002): $5000")
        println("Card 2: 9876543210987654 (PIN: 5678)")
        println("  - Checking Account (ACC003): $2000")
        println("  - Savings Account (ACC004): $10000")
    }
}
