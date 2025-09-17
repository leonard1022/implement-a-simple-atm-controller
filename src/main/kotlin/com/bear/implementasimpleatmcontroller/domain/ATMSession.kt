package com.bear.implementasimpleatmcontroller.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "atm_sessions")
class ATMSession(
    @Column(nullable = false, unique = true)
    val sessionId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    var card: Card? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var selectedAccount: Account? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var sessionStatus: SessionStatus = SessionStatus.CARD_INSERTED,

    @Column(nullable = false)
    var pinAttempts: Int = 0,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var closedAt: LocalDateTime? = null
) : BaseEntity() {

    fun incrementPinAttempts() {
        pinAttempts++
        if (pinAttempts >= MAX_PIN_ATTEMPTS) {
            sessionStatus = SessionStatus.CARD_BLOCKED
        }
    }

    fun closeSession() {
        sessionStatus = SessionStatus.CLOSED
        closedAt = LocalDateTime.now()
    }

    companion object {
        const val MAX_PIN_ATTEMPTS = 3
    }
}

enum class SessionStatus {
    CARD_INSERTED,
    PIN_VERIFIED,
    ACCOUNT_SELECTED,
    CARD_BLOCKED,
    CLOSED
}