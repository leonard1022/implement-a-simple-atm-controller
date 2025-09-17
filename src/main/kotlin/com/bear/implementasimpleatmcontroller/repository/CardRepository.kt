package com.bear.implementasimpleatmcontroller.repository

import com.bear.implementasimpleatmcontroller.domain.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CardRepository : JpaRepository<Card, Long> {
    fun findByCardNumber(cardNumber: String): Card?
}