package kr.co.wanted.implementasimpleatmcontroller.domain

import jakarta.persistence.*

@Entity
@Table(name = "accounts")
class Account(
    @Column(nullable = false, unique = true, length = 20)
    val accountNumber: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val accountType: AccountType,

    @Column(nullable = false)
    var balance: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    val card: Card
) : BaseEntity() {

    fun deposit(amount: Int) {
        require(amount > 0) { "Deposit amount must be positive" }
        balance += amount
    }

    fun withdraw(amount: Int) {
        require(amount > 0) { "Withdrawal amount must be positive" }
        require(balance >= amount) { "Insufficient balance" }
        balance -= amount
    }
}

enum class AccountType {
    CHECKING,
    SAVINGS
}