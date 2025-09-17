package com.bear.implementasimpleatmcontroller.repository

import com.bear.implementasimpleatmcontroller.domain.Account
import com.bear.implementasimpleatmcontroller.domain.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNumber(accountNumber: String): Account?
    fun findByCard(card: Card): List<Account>
}