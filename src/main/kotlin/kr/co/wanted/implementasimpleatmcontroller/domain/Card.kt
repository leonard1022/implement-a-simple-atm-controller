package kr.co.wanted.implementasimpleatmcontroller.domain

import jakarta.persistence.*

@Entity
@Table(name = "cards")
class Card(
    @Column(nullable = false, unique = true, length = 16)
    val cardNumber: String,

    @Column(nullable = false, length = 4)
    val pin: String,

    @Column(nullable = false, length = 100)
    val cardHolderName: String,

    @Column(nullable = false)
    var isActive: Boolean = true
) : BaseEntity()