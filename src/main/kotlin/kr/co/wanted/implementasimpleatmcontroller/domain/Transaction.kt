package kr.co.wanted.implementasimpleatmcontroller.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "transactions")
class Transaction(
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,

    @Column(nullable = false)
    val amount: Int,

    @Column(nullable = false)
    val balanceAfter: Int,

    @Column(nullable = false)
    val transactionDate: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account
) : BaseEntity()

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    BALANCE_INQUIRY
}