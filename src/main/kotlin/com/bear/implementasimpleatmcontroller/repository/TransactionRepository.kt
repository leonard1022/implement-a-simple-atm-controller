package com.bear.implementasimpleatmcontroller.repository

import com.bear.implementasimpleatmcontroller.domain.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
}
